<required_reading>
- `references/local-services.md` — local Vault/Cloudserver/OSIS must be running on the Mac before any of this works.
- `references/install-sequence.md` — phase-by-phase commands to run on the VM.
- `references/troubleshooting.md` — known gotchas; consult on any failure.
- `references/session-state-template.md` — write a fresh copy at the end.
</required_reading>

<prereqs>
Before touching AWS:

1. **Local services live.** Confirm `tmux ls` shows `vault`, `cloudserver`, `osis`, `ngrok`. If any are missing, walk the user through `references/local-services.md` first.

2. **ngrok URLs in `lab.yaml`.** Read `dev/vcd-ose-lab/configs/lab.yaml`. The `endpoints.osis_url` and `endpoints.s3_url` must point to live ngrok hostnames, and **the S3 ngrok hostname must be in `cloudserver/config.json` under `restEndpoints`** (otherwise S3 returns `Could not parse the specified URI`). If the user hasn't restarted ngrok recently and the URLs in `lab.yaml` look stale, ask them.

3. **Preflight green.** Run `cd dev/vcd-ose-lab && mage preflight`. Every row must be `[OK]`. Surface any `[FAIL]` to the user with the action line verbatim and stop.

4. **Confirm scope and cost.** Before `terraform apply`, tell the user: "About to create a c5.4xlarge in eu-north-1 (~$0.85/hr). Proceed?" Wait for explicit yes.
</prereqs>

<process>
## Phase 1 — AWS provisioning

```bash
cd dev/vcd-ose-lab
mage tfvars                                   # renders configs/lab.yaml → terraform/terraform.tfvars
terraform -chdir=terraform init -input=false  # idempotent
terraform -chdir=terraform plan -out=tfplan   # show user before applying
# After user confirms:
terraform -chdir=terraform apply tfplan
```

Read outputs:
```bash
terraform -chdir=terraform output -json
```

Capture `instance_id`, `private_ip`, `ami_id` into `_capture/aws-notes.md` (append) and the running session-state.

Wait for SSH to be reachable. First boot is ~55 seconds. Loop:

```bash
for i in {1..20}; do
  ssh -q -o ConnectTimeout=5 -i <key> rocky@<private_ip> 'echo READY' && break
  sleep 5
done
```

## Phase 2 — Open the shared remote tmux session

This is how the user attaches and watches. **All later commands go through this tmux**, not raw SSH `cmd` invocations.

```bash
ssh -q -i <key> rocky@<ip> 'sudo dnf install -y tmux'
ssh -q -i <key> rocky@<ip> 'tmux new-session -d -s lab "bash"'
ssh -q -i <key> rocky@<ip> 'tmux pipe-pane -t lab "cat >> /tmp/lab.log"'
```

Tell the user: "Attach with `ssh -i <key> -t rocky@<ip> 'tmux attach -t lab'` to watch."

For each remote command, send it via:
```bash
ssh -q -i <key> rocky@<ip> "tmux send-keys -t lab '<command>; echo __DONE_<id>_\$?' Enter"
```
then poll `tmux capture-pane -t lab -p -S -500` for `__DONE_<id>_` and parse the exit code.

## Phase 3 — Host bootstrap + PostgreSQL

Become root and install packages. **Note the Rocky-9 specific bits**:
- `dnf module enable postgresql:15` (not `:13` — doc gap #1).
- Must include `initscripts` so `/etc/init.d/functions` exists; without it, vmware-vcd refuses to start later (doc gap #6).

In tmux:
```
sudo -i
sed -i 's/^.*ssh-rsa/ssh-rsa/' ~/.ssh/authorized_keys
dnf update -y
dnf module enable postgresql:15 -y
dnf install -y vim nc bind bind-utils net-tools postgresql-server jq initscripts
```

Then run the postgres setup script. SCP it from the local repo and execute:
```bash
scp -i <key> dev/vcd-ose-lab/scripts/vm/postgres-setup.sh rocky@<ip>:/tmp/
ssh ... 'tmux send-keys -t lab "bash /tmp/postgres-setup.sh" Enter'
```

`postgres-setup.sh` does: initdb, overwrite `pg_hba.conf`, sed `postgresql.conf`, `systemctl start postgresql && enable`, create `vcdadmin`/`oseadmin` + `vcddb`/`osedb`, set `osedb` collation to `C`.

## Phase 4 — VCD install (the .bin)

SCP the binaries (don't forget `sudo mv` to `/root/` since `scp` lands them in `/home/rocky/`):
```bash
scp -i <key> dev/vcd-ose-lab/binaries/vmware-vcloud-director-distribution-*.bin rocky@<ip>:/home/rocky/
ssh ... 'sudo mv /home/rocky/vmware-vcloud-director-*.bin /root/'
```

In tmux, run the installer with **stdin piped** to skip both interactive prompts:
```
chmod u+x /root/vmware-vcloud-director-distribution-*.bin
printf 'y\nn\n' | /root/vmware-vcloud-director-distribution-*.bin
```

`y` answers "You are not running a supported Linux distribution. Proceed anyway?". `n` answers "Would you like to run the configuration script now?" (we run it ourselves in the next phase).

You will see `/bin/ln: failed to create symbolic link '/etc/init.d/'`. That's expected — doc gap #6. The systemd unit is missing too; we'll install it manually.

## Phase 5 — VCD configure

```
export HOST_IP=$(hostname -I | awk '{print $1}')
export FQDN=$(hostname -f)
cd /opt/vmware/vcloud-director/
/opt/vmware/vcloud-director/bin/cell-management-tool generate-certs \
  --cert cert.pem --key cert.key --key-password passwd
```

The configure command takes a long flag list — read it from `references/install-sequence.md` (Phase 5). Use values from `lab.yaml`: `postgres_password`, `--primary-ip $HOST_IP`.

After `configure` finishes (it prints `Database configuration complete.` and a non-fatal `chkconfig: No such file or directory`), install the systemd unit and start VCD:

```bash
scp -i <key> dev/vcd-ose-lab/scripts/vm/install-vmware-vcd-systemd-unit.sh rocky@<ip>:/tmp/
ssh ... 'tmux send-keys -t lab "bash /tmp/install-vmware-vcd-systemd-unit.sh && systemctl start vmware-vcd" Enter'
```

Wait until `cell.log` shows `Cell startup completed` (takes ~110 seconds). Poll:
```bash
ssh ... 'sudo grep -c "Cell startup completed" /opt/vmware/vcloud-director/logs/cell.log'
```

Then run `system-setup` with **password piped twice + Y for confirmation**:
```
printf "<vcd_admin_password>\n<vcd_admin_password>\nY\n" | \
  /opt/vmware/vcloud-director/bin/cell-management-tool system-setup \
  --user admin \
  --full-name "VCD System Administrator" \
  --email vcd-admin@scality.lab \
  --system-name VCD \
  --installation-id 2
```

Finally, **populate the `sites` table to make the H5 UI work** (doc gap #9). SCP + run:
```bash
scp -i <key> dev/vcd-ose-lab/scripts/vm/fix-vcd-public-urls.sh rocky@<ip>:/tmp/
ssh ... 'tmux send-keys -t lab "bash /tmp/fix-vcd-public-urls.sh" Enter'
```

That script does `UPDATE sites SET rest_api_endpoint='https://<FQDN>', base_ui_endpoint='https://<FQDN>', multisite_url='https://<FQDN>', name='VCD' WHERE is_local_site=true` and restarts vmware-vcd.

Wait again for `Cell startup completed`.

## Phase 6 — OSE install + EULA

```bash
scp -i <key> dev/vcd-ose-lab/binaries/vmware-ose-*.rpm rocky@<ip>:/home/rocky/
ssh ... 'sudo mv /home/rocky/vmware-ose-*.rpm /root/'
```

In tmux:
```
cd /root && dnf install -y ./vmware-ose-*.rpm
echo accept | ose -h
```

The `echo accept` persists EULA acceptance to `/opt/vmware/voss/agreement`. Subsequent ose commands won't re-prompt (doc gap #11).

## Phase 7 — OSE configure

SCP the PKCS12-build script:
```bash
scp -i <key> dev/vcd-ose-lab/scripts/vm/build-pkcs12-from-vcd-cert.sh rocky@<ip>:/tmp/
```

This script wraps the openssl + `ose cert import` pair, with the two fixes from doc gaps #10 and #13: `-passin pass:passwd` (so it doesn't prompt for the input key passphrase) and `-name <FQDN>` (so the keystore alias matches the cert CN — critical for browser trust later).

```
bash /tmp/build-pkcs12-from-vcd-cert.sh
ose db set --url jdbc:postgresql://localhost:5432/osedb --user oseadmin --secret 'Scality@123'
```

Then `ose director set` — **interactive in tmux**, not piped, because OSE prompts to trust the VCD cert with `y/N` and that prompt eats piped stdin:

```
ose director set --url https://$FQDN --user admin@system
# at "Secret :", type the VCD admin password
# at "Do you trust this certificate for the SSL connection?", type y
```

The agent should send these via separate `tmux send-keys` after polling capture-pane for each prompt. If you pipe both at once via `printf`, the trust-cert prompt fails with "Fail to set Cloud Director connection as EOF" (doc gap #11).

```
ose endpoint set --region=us-east-1 --url=https://$FQDN:8443
ose ui install
```

## Phase 8 — OSIS wiring

Read `endpoints.osis_url`, `endpoints.s3_url`, `secrets.osis_super_admin_access_key`, `secrets.osis_super_admin_secret_key` from `lab.yaml`.

Run interactively in tmux:
```
ose osis admin set --name scality --url <osis_url> --user <access_key>
# at "Secret :", type <secret_key>
# at "The given password does not meet the complexity criteria...", type y
ose osis s3 set --name scality --url <s3_url>
ose platforms enable osis --name scality
ose args set -k server.port -v 8443
systemctl start voss-keeper
ose service restart
```

Then `ose config validate`. Expect **all seven rows Valid**:
```
OSE Endpoint, Database, Certificate, Cloud Director, Platform-scality, Admin, S3
```

If any row is not Valid, jump to `references/troubleshooting.md` matching the row name.

## Phase 9 — Make the H5 UI usable from the Mac

Print these commands for the user to run on their Mac (the skill cannot do these — they need sudo on the user's machine):

```bash
echo "<private_ip>  <FQDN>" | sudo tee -a /etc/hosts
ssh -i <key> rocky@<private_ip> 'sudo cp /opt/vmware/vcloud-director/cert.pem /tmp/vcd-cert.pem && sudo chmod 644 /tmp/vcd-cert.pem'
scp -i <key> rocky@<private_ip>:/tmp/vcd-cert.pem ~/Downloads/vcd-lab.pem
sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain ~/Downloads/vcd-lab.pem
```

Then they can browse `https://<FQDN>/provider`, log in with `admin / <vcd_admin_password>`, and the More → Object Storage panel should load without CORS errors or self-signed warnings.

## Phase 10 — Capture state

Write `_capture/session-state.local.md` from `references/session-state-template.md`, filled in with: instance ID, private IP, FQDN, current ngrok URLs, super-admin access key (not secret), key file path, date. This file is gitignored.

Append to `_capture/aws-notes.md` a brief line with the date and AMI/region used.
</process>

<success_criteria>
- `ose config validate` on the VM shows all seven rows `Valid`.
- The user confirms `https://<FQDN>/provider` loads, More → Object Storage opens, and creating an S3 credential succeeds (no NPE).
- `_capture/session-state.local.md` is up-to-date.
- Total wall-clock: 25–35 minutes (binaries SCP ~5-10, VCD configure ~3-5, VCD boot ~2-3, OSE configure ~3-5, the rest is human-paced).
</success_criteria>
