# Deltas from the Confluence source doc

Tracked while running the lab on Rocky 9 (doc was written for Rocky 8).

| # | Doc says | Reality | Resolution |
|---|---|---|---|
| 1 | `dnf module enable postgresql:13` | Rocky 9 ships only `postgresql:15` and `postgresql:16`. PG 13 reached EOL 2025-11. | Use `postgresql:15` (VCD 10.5 documents PG 13–15 compat). |
| 2 | OSE RPM filename in doc: `vmware-ose-2.2.2-22098306.el7.x86_64.rpm` | We have `vmware-ose-3.0.0-23443325.el8.x86_64.rpm` | Use the newer 3.0 / el8 build; expect command surface differences vs the doc's OSE 2.x examples. To verify during install. |
| 3 | Doc instructs `sudo su -` then disconnect and SSH as root | We stay in tmux and use `sudo -i` once; no SSH reconnect needed | The `sed -i 's/^.*ssh-rsa/ssh-rsa/' authorized_keys` step is still useful for future direct-root SSH but not required for the install flow. |
| 4 | Doc shows firewall step "should be disabled, but if not..." | Rocky 9 AMI has no firewalld unit | `systemctl stop firewalld` returns non-zero; script must tolerate. |
| 5 | n/a (capture tooling) | `tmux send-keys` mangles SQL single quotes (escapes are interpreted at the wrong layer) | Drop multi-line scripts to `/tmp/*.sh` via SSH `cat <<'EOS'`, then `bash /tmp/*.sh`. Codify in install scripts. |
