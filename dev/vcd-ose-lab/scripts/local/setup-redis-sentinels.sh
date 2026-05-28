#!/bin/bash
# Runs on the Mac. Starts three Redis sentinels monitoring localhost:6379
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
    if [ -f "${pidfile}" ]; then
        kill "$(cat "${pidfile}")" 2>/dev/null || true
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
