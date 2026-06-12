#!/bin/bash
# Runs on the VM as root. Initializes PostgreSQL, applies a lab-grade
# pg_hba.conf (local-only with scram-sha-256 — VCD and OSE live on the
# same host as PG), patches postgresql.conf, starts the service, and
# creates the two roles + databases (osedb with C collation, required
# by OSE).
#
# PG_PASSWORD must be supplied via env (no default). Single quotes inside
# the value are safe — we pass it via psql's `-v` variable substitution
# (the `:'password'` form) which quotes correctly.

set -euo pipefail

: "${PG_PASSWORD:?PG_PASSWORD must be set in the environment}"

if [ ! -f /var/lib/pgsql/data/PG_VERSION ]; then
    postgresql-setup --initdb
fi

cat > /var/lib/pgsql/data/pg_hba.conf <<'EOF'
local   all             all                                     peer
host    all             all             127.0.0.1/32            scram-sha-256
host    all             all             ::1/128                 scram-sha-256
EOF

sed -i "s/#listen_addresses = 'localhost'/listen_addresses = 'localhost'/" /var/lib/pgsql/data/postgresql.conf
sed -i "s/max_connections = 100[[:space:]]*# (change requires restart)/max_connections = 300/" /var/lib/pgsql/data/postgresql.conf
sed -i "s/#superuser_reserved_connections = 3[[:space:]]*# (change requires restart)/superuser_reserved_connections = 90/" /var/lib/pgsql/data/postgresql.conf
sed -i "s/#password_encryption = scram-sha-256/password_encryption = scram-sha-256/" /var/lib/pgsql/data/postgresql.conf

systemctl enable --now postgresql

nc -vz localhost 5432

# The scram-only pg_hba.conf and the psql features below assume PG 15
# (the spin-up workflow enables the postgresql:15 module). On an older
# server the password_encryption sed above silently no-ops and every
# host login would be rejected — fail fast instead.
server_num="$(sudo -u postgres psql -tAc 'show server_version_num')"
if [ "${server_num}" -lt 150000 ]; then
    echo "ERROR: PostgreSQL 15+ required (server_version_num=${server_num})." >&2
    echo "Run: dnf module enable postgresql:15 -y && dnf install -y postgresql-server" >&2
    exit 1
fi

# Use psql's `-v` to pass the password and `:'name'` substitution to
# quote/escape it safely — no shell interpolation into the SQL. The
# substitution must happen at the top level (psql skips it inside
# dollar-quoted DO blocks), so build each CREATE USER with format(%L)
# and run it via \gexec.
sudo -u postgres psql -v ON_ERROR_STOP=1 -v password="${PG_PASSWORD}" <<'SQL'
SELECT format('CREATE USER vcdadmin WITH PASSWORD %L LOGIN', :'password')
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'vcdadmin') \gexec
SELECT format('CREATE USER oseadmin WITH PASSWORD %L LOGIN', :'password')
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'oseadmin') \gexec
-- Re-runs with a different PG_PASSWORD must take effect, not silently
-- keep the old one.
SELECT format('ALTER USER vcdadmin WITH PASSWORD %L', :'password') \gexec
SELECT format('ALTER USER oseadmin WITH PASSWORD %L', :'password') \gexec
SQL

sudo -u postgres psql -tAc "SELECT 1 FROM pg_database WHERE datname='vcddb'" | grep -q 1 \
  || sudo -u postgres psql -c "CREATE DATABASE vcddb OWNER vcdadmin;"
# OSE requires C collation. Set it at creation time (TEMPLATE template0 is
# required when the collation differs from the server default) — updating
# pg_database after the fact only rewrites catalog metadata.
sudo -u postgres psql -tAc "SELECT 1 FROM pg_database WHERE datname='osedb'" | grep -q 1 \
  || sudo -u postgres psql -c "CREATE DATABASE osedb OWNER oseadmin LC_COLLATE 'C' LC_CTYPE 'C' TEMPLATE template0;"

echo '--- post-setup ---'
sudo -u postgres psql -c "\l" | grep -E '(vcddb|osedb)'
