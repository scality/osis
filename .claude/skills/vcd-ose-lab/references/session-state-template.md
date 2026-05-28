# Session state (template)

Copy this template to `dev/vcd-ose-lab/_capture/session-state.local.md` (gitignored via `_capture/*.local.md`) at the end of a `spin-up` workflow, and update the URL block on every `refresh-endpoints`.

```markdown
# Live session state (local, gitignored)

Date: <YYYY-MM-DD>

## EC2 lab

| | |
|---|---|
| Instance | `<instance_id>` |
| Private IP | `<private_ip>` |
| FQDN | `ip-<a>-<b>-<c>-<d>.<region>.compute.internal` |
| Region | `<region>` |
| AMI | `<ami_id>` |
| Key | `<key_path>` |
| SSH | `ssh -i <key> rocky@<ip>` |
| Remote tmux | `ssh -i <key> -t rocky@<ip> 'tmux attach -t lab'` |
| VCD provider UI | `https://<FQDN>/provider` (admin / <vcd_admin_password>) |
| OSE endpoint | `https://<FQDN>:8443` |
| VCD admin user | `admin@system` |
| Cert passphrase | `passwd` |

Mac /etc/hosts:
```
<private_ip>  <FQDN>
```

## Local Mac services (tmux sessions)

| Session | Service | Port | Attach |
|---|---|---|---|
| `vault` | Vault | 8500, 8600 | `tmux attach -t vault` |
| `cloudserver` | S3 | 8000 | `tmux attach -t cloudserver` |
| `osis` | OSIS (Java) | 9333 | `tmux attach -t osis` |
| `ngrok` | ngrok | 4040 | `tmux attach -t ngrok` |

Redis sentinels (daemonized): 26379, 26380, 26381.

## ngrok tunnels (change every restart)

| | |
|---|---|
| OSIS public URL | `<osis_ngrok_url>` → localhost:9333 |
| S3 public URL | `<s3_ngrok_url>` → localhost:8000 |

If these change, run the `refresh-endpoints` workflow.

## OSIS super-admin creds (for `ose osis admin set`)

Access key: `<access_key>`
(Secret in `dev/vcd-ose-lab/configs/lab.yaml`, never write the secret here.)
```
