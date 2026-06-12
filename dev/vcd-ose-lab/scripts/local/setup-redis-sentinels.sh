#!/bin/bash
# Runs on the dev machine (Mac or Linux). Starts three Redis sentinels monitoring localhost:6379
# (the master). OSIS hardcodes Redis Sentinel mode and won't accept plain
# standalone Redis (see SpringRedisConfig.java).
#
# Idempotent: kills any existing sentinel on 26379/26380/26381 before
# starting fresh.

set -euo pipefail

DIR="${OSIS_LOCAL_DIR:-/tmp/osis-local}/sentinels"
mkdir -p "${DIR}"

for p in 26379 26380 26381; do
    pidfile="${DIR}/sentinel-${p}.pid"
    # Stop whatever holds the port, not just what the pidfile knows about
    # (the pidfile can be stale or gone while a sentinel still runs).
    redis-cli -p "${p}" shutdown nosave 2>/dev/null || true
    if [ -f "${pidfile}" ]; then
        oldpid="$(cat "${pidfile}")"
        kill "${oldpid}" 2>/dev/null || true
        # Wait for the old process to release the socket: daemonized redis
        # exits 0 before binding, so starting the replacement too early
        # fails silently (the bind error only lands in the logfile).
        for _ in $(seq 1 50); do
            kill -0 "${oldpid}" 2>/dev/null || break
            sleep 0.1
        done
        rm -f "${pidfile}"
    fi

    cat > "${DIR}/sentinel-${p}.conf" <<EOF
port ${p}
dir ${DIR}
pidfile ${pidfile}
logfile ${DIR}/sentinel-${p}.log
sentinel monitor mymaster 127.0.0.1 6379 2
sentinel down-after-milliseconds mymaster 5000
sentinel failover-timeout mymaster 60000
sentinel parallel-syncs mymaster 1
EOF

    redis-sentinel "${DIR}/sentinel-${p}.conf" --daemonize yes
done

sleep 1
for p in 26379 26380 26381; do
    printf 'sentinel %s: ' "${p}"
    redis-cli -p "${p}" ping
done
