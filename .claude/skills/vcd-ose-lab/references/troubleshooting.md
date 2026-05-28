# Troubleshooting — known gotchas

Match the symptom to an entry below before improvising. Numbers correspond to `_capture/DOC_GAPS.md`.

---

**Symptom:** `dnf module enable postgresql:13` says "no such module".
**Cause (#1):** Rocky 9 only ships `postgresql:15` and `:16` (PG 13 EOL'd Nov 2025).
**Fix:** Use `postgresql:15`. VCD 10.5 is documented to work with PG 13–15.

---

**Symptom:** OSE RPM in the doc is `2.2.2-22098306.el7.x86_64.rpm`; you have a newer one like `3.0.0-23443325.el8.x86_64.rpm`.
**Cause (#2):** The build moved on.
**Fix:** Use whichever RPM is in your Drive. The CLI surface has minor changes (extra EULA-persist file, extra cert-trust prompt in `ose director set`) — captured throughout this doc.

---

**Symptom:** Doc says `service vmware-vcd start`; it returns `Unit vmware-vcd.service not found.`
**Cause (#6):** The VCD installer skips creating the systemd unit on Rocky 9 because it tries to call `/sbin/chkconfig` which doesn't exist. The installer ALSO tries to symlink `/etc/init.d/` which doesn't exist.
**Fix:** Run `scripts/vm/install-vmware-vcd-systemd-unit.sh`. Also ensure `initscripts` is installed beforehand so `/etc/init.d/functions` exists (the VCD init script sources it).

---

**Symptom:** `vmware-vcd.service` fails to start with `ERROR: Unable to source system init functions, cannot proceed`.
**Cause (#6, second half):** `initscripts` package missing → `/etc/init.d/functions` missing.
**Fix:** `dnf install -y initscripts` then `systemctl start vmware-vcd`.

---

**Symptom:** `https://<vm-ip>/provider` returns "Failed to Start: An error occurred during the initialization. Accessing the application through an unsupported public URL or poor connectivity might cause this error."
**Cause (#9):** Two things in series. (a) The `sites` table has empty `rest_api_endpoint` / `base_ui_endpoint` / `multisite_url` — `system-setup` doesn't populate them. (b) Even after populating, accessing via the raw IP fails — the H5 UI only initializes cleanly via the FQDN that matches the cert CN.
**Fix:**
1. Run `scripts/vm/fix-vcd-public-urls.sh` (populates the table to the FQDN URL, restarts vmware-vcd).
2. On the Mac: `echo "<ip> <FQDN>" | sudo tee -a /etc/hosts`, trust the VCD cert in Keychain, browse `https://<FQDN>/provider`.

---

**Symptom:** `ose director set` fails with `Fail to set Cloud Director connection as EOF`, even with the password piped via `printf`.
**Cause (#11):** OSE 3.0 prompts to **trust the self-signed VCD cert** with `y/N` *after* asking for the password. If you pipe only the password, the trust-cert prompt hits EOF and the connection setup fails.
**Fix:** Run `ose director set` interactively. Send via two separate `tmux send-keys`: first the password, then `y`. Or extend the pipe: `printf "<password>\ny\n" | ose director set ...` if you're sure the order is right (it usually is).

---

**Symptom:** "Object Storage Extension service endpoint is found not accessible from your web browser" when clicking More → Object Storage in VCD.
**Cause (#13):** OSE endpoint URL is set to the IP, but the user's Mac trusts the cert by FQDN (or vice-versa). The cert SAN does include both, but Mac keychain trust + browser strict SAN matching only line up reliably with FQDN.
**Fix:**
1. Re-issue OSE PKCS12 with `-name <FQDN>` (use `scripts/vm/build-pkcs12-from-vcd-cert.sh`).
2. `ose endpoint set --region=us-east-1 --url=https://<FQDN>:8443`.
3. `ose service restart`.

---

**Symptom:** Object Storage panel loads partially, browser console shows CORS errors on `xhr` calls to OSE (e.g., `current-user`).
**Cause (#14):** OSE allows CORS only for the URL configured as the Cloud Director. If VCD is accessed at the FQDN but `ose director set` was last called with the IP, the browser's `Origin: https://<FQDN>` header doesn't match OSE's allowlisted origin.
**Fix:** Re-run `ose director set --url https://<FQDN> --user admin@system` (interactive — provide password, then `y` if it re-prompts cert trust; usually it won't, since OSE remembers). `ose service restart`.

---

**Symptom:** `Create S3 Credential error. NullPointerException: Cannot invoke "java.util.List.stream()" because the return value of "com.scality.osis.security.crypto.CryptoEnv.getKeys()" is null` in the OSIS log.
**Cause (#N/A — local-dev specific):** OSIS reads `crypto.yml` from a hardcoded path. By default that path is `file:/conf/crypto.yml`. The branch in use should have it overridable via `osis.security.crypto-config-path` (see CryptoEnv.java); make sure your local `application.properties` sets that, **or** place `crypto.yml` at `/conf/crypto.yml`.
**Fix:** Either edit `/tmp/osis-local/application.properties` to add `osis.security.crypto-config-path=file:/tmp/osis-local/crypto.yml`, or `sudo cp /tmp/osis-local/crypto.yml /conf/crypto.yml`. Then restart OSIS in its tmux.

---

**Symptom:** `Could not parse the specified URI. Check your restEndpoints configuration.` from S3 when OSE talks to it.
**Cause:** The S3 ngrok hostname isn't in cloudserver's `restEndpoints` map. Cloudserver routes by Host header and rejects unknown hostnames.
**Fix:** Add the S3 ngrok hostname to `~/anurag-builds-things/scality/cloudserver/config.json` under `restEndpoints`, value `"us-east-1"`. Restart cloudserver in its tmux.

---

**Symptom:** OSIS log says `Cannot invoke "...RedisProperties$Sentinel.getMaster()" because the return value of "...getSentinel()" is null`.
**Cause:** OSIS's `SpringRedisConfig` hardcodes Redis Sentinel mode. Plain `spring.redis.host` doesn't work.
**Fix:** Start three Redis sentinels (`scripts/local/setup-redis-sentinels.sh`) and use `spring.redis.sentinel.master=mymaster` + `spring.redis.sentinel.nodes=localhost:26379,localhost:26380,localhost:26381` in `application.properties`.

---

**Symptom:** tmux pane has stale `^[[18;162R^[[18;162R` polluting subsequent commands.
**Cause (#12):** `ose` emits ANSI cursor-position queries; the terminal replies with bytes that leak into stdin.
**Fix:** Send `reset` to the tmux pane. The skill should do this after any sequence of `ose` commands if it intends to send-keys more commands after.
