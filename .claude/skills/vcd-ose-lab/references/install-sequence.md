# Install sequence (Rocky 9 + VCD 10.5 + OSE 3.0)

Distilled from `_capture/SHELL_HISTORY.md`. Every line here was verified end-to-end on 2026-05-28. Where the Confluence doc differs, the doc-gap number in parentheses points at `_capture/DOC_GAPS.md` and `references/troubleshooting.md`.

All commands assume you are root in the remote `lab` tmux session.

## Phase 3 — OS bootstrap

```bash
sed -i 's/^.*ssh-rsa/ssh-rsa/' ~/.ssh/authorized_keys           # for direct-root-ssh later (cosmetic)
dnf update -y
dnf module enable postgresql:15 -y                              # (#1: not :13 on Rocky 9)
dnf install -y vim nc bind bind-utils net-tools \
    postgresql-server jq initscripts                            # (#6: initscripts required)
```

## Phase 3b — PostgreSQL

Run `scripts/vm/postgres-setup.sh`. Equivalent steps:

```bash
postgresql-setup --initdb
# Overwrite pg_hba.conf with the doc's permissive 5-line variant
# Patch postgresql.conf: listen_addresses='*', max_connections=300, superuser_reserved_connections=90
systemctl start postgresql && systemctl enable postgresql

sudo -u postgres psql <<'SQL'
create user vcdadmin with password 'Scality@123' login;
create user oseadmin with password 'Scality@123' login;
create database vcddb owner vcdadmin;
create database osedb owner oseadmin;
update pg_database set datcollate='C', datctype='C' where datname='osedb';
SQL
```

(#5: send SQL via `psql <<'SQL'` heredoc in a script, not via `tmux send-keys` — single quotes get mangled.)

## Phase 4 — VCD install

```bash
chmod u+x /root/vmware-vcloud-director-distribution-*.bin
printf 'y\nn\n' | /root/vmware-vcloud-director-distribution-*.bin
```

Expect a non-fatal `/bin/ln: failed to create symbolic link '/etc/init.d/'`. The RPMs install successfully but no systemd unit is created (#6).

## Phase 5 — VCD configure

```bash
export HOST_IP=$(hostname -I | awk '{print $1}')
export FQDN=$(hostname -f)

cd /opt/vmware/vcloud-director/
/opt/vmware/vcloud-director/bin/cell-management-tool generate-certs \
    --cert cert.pem --key cert.key --key-password passwd

/opt/vmware/vcloud-director/bin/configure \
  --cert /opt/vmware/vcloud-director/cert.pem \
  --key /opt/vmware/vcloud-director/cert.key \
  --key-password passwd \
  --primary-ip $HOST_IP \
  --primary-port-http 80 \
  --primary-port-https 443 \
  --database-type postgres \
  --database-host localhost \
  --database-port 5432 \
  --database-name vcddb \
  --database-user vcdadmin \
  --database-password 'Scality@123' \
  --enable-ceip false \
  --unattended-installation
```

Expect a non-fatal `/sbin/chkconfig: No such file or directory` (#7). Then install the systemd unit:

```bash
bash /tmp/install-vmware-vcd-systemd-unit.sh
systemctl start vmware-vcd
```

Wait for `Cell startup completed` in `/opt/vmware/vcloud-director/logs/cell.log` (~110 seconds).

Then `system-setup` with stdin piped:

```bash
printf "<vcd_admin_password>\n<vcd_admin_password>\nY\n" | \
  /opt/vmware/vcloud-director/bin/cell-management-tool system-setup \
  --user admin \
  --full-name "VCD System Administrator" \
  --email vcd-admin@scality.lab \
  --system-name VCD \
  --installation-id 2
```

Then populate the `sites` table so the H5 UI works (#9), and restart:

```bash
bash /tmp/fix-vcd-public-urls.sh
# scripts/vm/fix-vcd-public-urls.sh runs UPDATE sites SET rest_api_endpoint=...
# and restarts vmware-vcd
```

## Phase 6 — OSE install

```bash
cd /root && dnf install -y ./vmware-ose-*.rpm
echo accept | ose -h                       # (#11: persists EULA to /opt/vmware/voss/agreement)
```

## Phase 7 — OSE configure

```bash
bash /tmp/build-pkcs12-from-vcd-cert.sh   # (#10: includes -passin; #13: FQDN friendly name)

ose db set --url jdbc:postgresql://localhost:5432/osedb \
           --user oseadmin --secret 'Scality@123'
```

`ose director set` must be **interactive** (#11): pipe the password, then send `y` for trust-cert separately.

```bash
ose director set --url https://$FQDN --user admin@system
# at "Secret :" → type <vcd_admin_password>
# at "Do you trust this certificate for the SSL connection?" → y
```

```bash
ose endpoint set --region=us-east-1 --url=https://$FQDN:8443
ose ui install
```

## Phase 8 — OSIS wiring

```bash
ose osis admin set --name scality --url <osis_ngrok_url> --user <access_key>
# at "Secret :" → type <secret_key>
# at "password does not meet the complexity criteria" → y

ose osis s3 set --name scality --url <s3_ngrok_url>
ose platforms enable osis --name scality
ose args set -k server.port -v 8443
systemctl start voss-keeper
ose service restart
ose config validate
```

Expect all seven rows `Valid`.

## Phase 9 — Mac-side cert trust + /etc/hosts

User-side commands (the agent prints, doesn't execute — they need sudo on the user's Mac):

```bash
echo "<private_ip>  <FQDN>" | sudo tee -a /etc/hosts

ssh -i <key> rocky@<private_ip> 'sudo cp /opt/vmware/vcloud-director/cert.pem /tmp/vcd-cert.pem && sudo chmod 644 /tmp/vcd-cert.pem'
scp -i <key> rocky@<private_ip>:/tmp/vcd-cert.pem ~/Downloads/vcd-lab.pem
sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain ~/Downloads/vcd-lab.pem
```

Then `https://<FQDN>/provider` works in the browser, and clicking More → Object Storage loads without CORS or trust errors.
