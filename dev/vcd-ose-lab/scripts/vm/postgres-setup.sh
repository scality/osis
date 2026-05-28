#!/bin/bash
# Runs on the VM as root. Initializes PostgreSQL, applies the doc's permissive
# pg_hba.conf, patches postgresql.conf, starts the service, creates the two
# roles + databases, and forces osedb collation to C (required by OSE).
#
# Idempotent enough: re-running on a fresh initdb fails fast; re-running on
# an already-initialized DB skips the user-creation if they exist.

set -euo pipefail

PG_PASSWORD="${PG_PASSWORD:-Scality@123}"

if [ ! -f /var/lib/pgsql/data/PG_VERSION ]; then
    postgresql-setup --initdb
fi

cat > /var/lib/pgsql/data/pg_hba.conf <<'EOF'
local   all             all                                     trust
host    all             all             127.0.0.1/32            trust
host    all             all             ::1/128                 trust
host    all             all             0.0.0.0/0               md5
host    all             all             ::/0                    md5
EOF

sed -i "s/#listen_addresses = 'localhost'/listen_addresses = '*'/" /var/lib/pgsql/data/postgresql.conf
sed -i "s/max_connections = 100[[:space:]]*# (change requires restart)/max_connections = 300/" /var/lib/pgsql/data/postgresql.conf
sed -i "s/#superuser_reserved_connections = 3[[:space:]]*# (change requires restart)/superuser_reserved_connections = 90/" /var/lib/pgsql/data/postgresql.conf

systemctl enable --now postgresql

# Tolerate firewalld being absent on the AMI (Rocky 9 EC2).
systemctl stop firewalld 2>/dev/null || true
systemctl disable firewalld 2>/dev/null || true

nc -vz localhost 5432

sudo -u postgres psql <<SQL
DO \$\$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'vcdadmin') THEN
    CREATE USER vcdadmin WITH PASSWORD '${PG_PASSWORD}' LOGIN;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'oseadmin') THEN
    CREATE USER oseadmin WITH PASSWORD '${PG_PASSWORD}' LOGIN;
  END IF;
END\$\$;
SQL

sudo -u postgres psql -tAc "SELECT 1 FROM pg_database WHERE datname='vcddb'" | grep -q 1 \
  || sudo -u postgres psql -c "CREATE DATABASE vcddb OWNER vcdadmin;"
sudo -u postgres psql -tAc "SELECT 1 FROM pg_database WHERE datname='osedb'" | grep -q 1 \
  || sudo -u postgres psql -c "CREATE DATABASE osedb OWNER oseadmin;"

sudo -u postgres psql -c "UPDATE pg_database SET datcollate='C', datctype='C' WHERE datname='osedb';"

echo '--- post-setup ---'
sudo -u postgres psql -c "\l" | grep -E '(vcddb|osedb)'
