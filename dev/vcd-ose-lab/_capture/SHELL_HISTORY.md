# Shell history: first VCD-OSE lab ride-along

VM: i-02728ce69c98ba53d (10.160.96.15), Rocky 9.7, eu-north-1
Date: 2026-05-28
All commands run inside remote tmux session `lab` on the VM.

---

## 1. Become root + allow root SSH

```
$ sudo -i
# whoami
root
# sed -i 's/^.*ssh-rsa/ssh-rsa/' ~/.ssh/authorized_keys
```

## 2. Probe available PostgreSQL modules (delta from doc)

```
# dnf module list postgresql
Rocky Linux 9 - AppStream
Name       Stream Profiles           Summary
postgresql 15     client, server [d] PostgreSQL server and client module
postgresql 16     client, server [d] PostgreSQL server and client module
```

Doc says `postgresql:13`; not available on Rocky 9. Picking **15**.

## 3. OS bootstrap

```
# dnf update -y && dnf module enable postgresql:15 -y && \
  dnf install vim nc bind bind-utils net-tools postgresql-server jq -y
```

Installed (PG-relevant): `postgresql-15.17`, `postgresql-server-15.17`, `postgresql-private-libs-15.17`. Plus the utility set from the doc.

## 4. PostgreSQL initdb

```
# postgresql-setup --initdb
 * Initializing database in '/var/lib/pgsql/data'
 * Initialized, logs are in /var/lib/pgsql/initdb_postgresql.log
```

## 5. PostgreSQL config

Wrote `/var/lib/pgsql/data/pg_hba.conf` (overwriting default) with:
```
local   all             all                                     trust
host    all             all             127.0.0.1/32            trust
host    all             all             ::1/128                 trust
host    all             all             0.0.0.0/0               md5
host    all             all             ::/0                    md5
```

Patched `/var/lib/pgsql/data/postgresql.conf`:
```
listen_addresses = '*'
max_connections = 300
superuser_reserved_connections = 90
```

The doc's three sed patterns matched PG 15's default config unchanged.

## 6. Start PostgreSQL + verify

```
# systemctl start postgresql && systemctl enable postgresql && systemctl is-active postgresql
active
# systemctl stop firewalld; systemctl disable firewalld
(firewalld already absent on this AMI; no-op)
# nc -vz localhost 5432
Ncat: Connected to ::1:5432.
```

## 7. Create roles + databases

Ran (as `postgres`):
```sql
create user vcdadmin with password 'Scality@123' login;
create user oseadmin with password 'Scality@123' login;
create database vcddb owner vcdadmin;
create database osedb owner oseadmin;
update pg_database set datcollate='C', datctype='C' where datname='osedb';
```

`\l` afterwards confirmed: `vcddb` (en_US.UTF-8 collation), `osedb` (C/C collation, as required by OSE).

**Quoting gotcha:** sending the SQL via `tmux send-keys` mangled single-quote escapes. Resolution: drop the SQL into `/tmp/pgusers.sh` with a `psql <<'SQL'` heredoc and execute. Codify this pattern in the install scripts.
