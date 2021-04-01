/**
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.vaultadmin.impl.cache;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.scality.osis.vaultadmin.impl.cache.CacheConstants.*;

/** CacheImpl is a thread-safe LRU cache implementation with expiration time for each entry
 * <p>
 * Using ConcurrentHashMap + ConcurrentLinkedQueue + ReadWriteLock + ScheduledExecutorService to implement thread-safe LRU caching
 */
public class CacheImpl<K, V> implements Cache<K,V> {

    /**
     * Maximum capacity of the cache
     */
    private final int maxCapacity;
    /**
     * expiration time for each entrant of the cache
     */
    private long expirationTime;
    private final Map<K, V> internalCache;
    private final Queue<K> trackingQueue;

    /**
     * Read-write lock
     */
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock writeLock = readWriteLock.writeLock();
    private final Lock readLock = readWriteLock.readLock();

    private final ScheduledExecutorService scheduledExecutorService;

    public CacheImpl(int maxCapacity){
        this(maxCapacity, DEFAULT_EXPIRY_TIME_IN_MS);
    }

    public CacheImpl(int maxCapacity, long expirationTime){
        if (maxCapacity < 0 || expirationTime < 0L) {
            throw new IllegalArgumentException("Illegal max capacity: " + maxCapacity);
        }

        this.maxCapacity = maxCapacity;
        this.expirationTime = expirationTime;
        internalCache = new ConcurrentHashMap<>(maxCapacity);
        trackingQueue = new ConcurrentLinkedQueue<>();
        scheduledExecutorService = Executors.newScheduledThreadPool(DEFAULT_SCHEDULED_THREAD_POOL_SIZE);
    }

    /**
     * @param key Key
     * @return Value corresponding to the Key K in the map is returned or null
     *         is returned if the key is not present in the map
     */
    @Override
    public V get(K key) {
        //Add read lock
        readLock.lock();
        try {
            //whether the key exists in the current cache
            V value = internalCache.get(key);
            if (value != null) {
                // If it exists, move the key to the end of the queue
                if (trackingQueue.remove(key)) {
                    trackingQueue.add(key);
                }
            }
            return value;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * @param key Key
     * @param value Value
     * Entry with key k and value v is added to the Cache.
     */
    @Override
    public V put(K key, V value) {
//        return put(key, value, DEFAULT_EXPIRY_TIME_IN_MS);
        // add write lock
        writeLock.lock();
        try {
            //1. If the key exists in the current cache, remove it from queue
            if (internalCache.containsKey(key)) {
                trackingQueue.remove(key);
            }
            //2. If the cache capacity is exceeded, remove the element at the head of the queue and cache
            if (trackingQueue.size() == maxCapacity) {
                K oldestKey = trackingQueue.poll();
                if (oldestKey != null) {
                    internalCache.remove(oldestKey);
                }
            }

            //3.key does not exist in the current cache. Add the key to the end of the queue and cache the key and its corresponding elements
            internalCache.put(key, value);
            trackingQueue.add(key);

            if (expirationTime > 0) {
                removeAfterExpireTime(key, expirationTime);
            }
        } finally {
            writeLock.unlock();
        }
        return value;
    }

    /**
     * @param key Key
     * @return Value corresponding to the key is returned if present in the map
     *         else null is returned. Entry with key k is removed from the
     *         cache.
     */
    @Override
    public V remove(K key) {
        writeLock.lock();
        try {
            //whether the key exists in the current cache
            if (internalCache.containsKey(key)) {
                // There is a corresponding Key in the removal queue and Map
                trackingQueue.remove(key);
                return internalCache.remove(key);
            }
            //Return Null if it does not exist in the current cache
            return null;
        } finally {
            writeLock.unlock();
        }

    }

    /**
     * Clears the cache.
     */
    @Override
    public void clear() {
        writeLock.lock();
        try {
            internalCache.clear();
            trackingQueue.clear();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public long size() {
        return internalCache.size();
    }

    private void removeAfterExpireTime(K key, long expireTime) {
        scheduledExecutorService.schedule(() -> {
            //Clear the key-value pair after expiration
            internalCache.remove(key);
            trackingQueue.remove(key);
        }, expireTime, TimeUnit.MILLISECONDS);
    }

    @Override
    public String toString() {
        return "CacheImpl{" +
                "internalCache=" + internalCache +
                '}';
    }
}
