# Install sequence (Rocky 9 + VCD 10.5 + OSE 3.0)

Distilled from the manual ride-along of 2026-05-28 — every line here was verified end-to-end. Where the Confluence doc differs, the doc-gap number in parentheses points at the matching entry in `references/troubleshooting.md`.

All commands assume you are root in the remote `lab` tmux session.

Placeholders like `<postgres_password>` and `<vcd_cert_passphrase>` come from `configs/lab.yaml` (`mage preflight` fails if they're unset). Never substitute literal example passwords.

## Phase 3 — OS bootstrap

```bash
sed -i 's/^.*ssh-rsa/ssh-rsa/' ~/.ssh/authorized_keys           # for direct-root-ssh later (cosmetic)
dnf update -y
dnf module enable postgresql:15 -y                              # (#1: not :13 on Rocky 9)
dnf install -y vim nc bind bind-utils net-tools \
    postgresql-server jq initscripts                            # (#6: initscripts required)
```

## Phase 3b — PostgreSQL

Run `PG_PASSWORD='<postgres_password>' bash scripts/vm/postgres-setup.sh` (the password is required via env; the script quotes it safely server-side). Equivalent steps:

```bash
postgresql-setup --initdb
# Write the local-only pg_hba.conf (peer for unix socket, scram-sha-256 for loopback)
# Patch postgresql.conf: listen_addresses='localhost', max_connections=300, superuser_reserved_connections=90
systemctl start postgresql && systemctl enable postgresql

sudo -u postgres psql <<'SQL'
create user vcdadmin with password '<postgres_password>' login;
create user oseadmin with password '<postgres_password>' login;
create database vcddb owner vcdadmin;
create database osedb owner oseadmin lc_collate 'C' lc_ctype 'C' template template0;
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
    --cert cert.pem --key cert.key --key-password '<vcd_cert_passphrase>'

/opt/vmware/vcloud-director/bin/configure \
  --cert /opt/vmware/vcloud-director/cert.pem \
  --key /opt/vmware/vcloud-director/cert.key \
  --key-password '<vcd_cert_passphrase>' \
  --primary-ip $HOST_IP \
  --primary-port-http 80 \
  --primary-port-https 443 \
  --database-type postgres \
  --database-host localhost \
  --database-port 5432 \
  --database-name vcddb \
  --database-user vcdadmin \
  --database-password '<postgres_password>' \
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
CERT_PASSPHRASE='<vcd_cert_passphrase>' bash /tmp/build-pkcs12-from-vcd-cert.sh
# (#10: includes -passin via env:CERT_PASSPHRASE; #13: FQDN friendly name)

ose db set --url jdbc:postgresql://localhost:5432/osedb \
           --user oseadmin --secret '<postgres_password>'
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

## Phase 9 — Dev-machine cert trust + /etc/hosts

User-side commands (the agent prints, doesn't execute — they need sudo on the user's box). Use `ca_trust_commands` from `scripts/local/lib/platform.sh` to emit the correct form for the detected OS.

Common steps (all platforms):

```bash
echo "<private_ip>  <FQDN>" | sudo tee -a /etc/hosts
ssh -i <key> rocky@<private_ip> 'sudo cp /opt/vmware/vcloud-director/cert.pem /tmp/vcd-cert.pem && sudo chmod 644 /tmp/vcd-cert.pem'
scp -i <key> rocky@<private_ip>:/tmp/vcd-cert.pem ~/Downloads/vcd-lab.pem
```

Then **one** of the following:

- **Mac:** `sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain ~/Downloads/vcd-lab.pem`
- **Debian/Ubuntu:** `sudo cp ~/Downloads/vcd-lab.pem /usr/local/share/ca-certificates/vcd-lab.crt && sudo update-ca-certificates`
- **Fedora/Rocky/RHEL:** `sudo cp ~/Downloads/vcd-lab.pem /etc/pki/ca-trust/source/anchors/vcd-lab.pem && sudo update-ca-trust`

After that `https://<FQDN>/provider` works in the browser, and More → Object Storage loads without CORS or trust errors.

Firefox on Linux maintains its own NSS trust store — if the user opens VCD in Firefox they also need to import the PEM under Settings → Privacy & Security → View Certificates → Import. Chrome/Chromium reads the system NSS DB so the steps above are enough for those browsers.
