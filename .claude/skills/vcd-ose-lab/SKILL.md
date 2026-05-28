---
name: vcd-ose-lab
description: Spin up, refresh, tear down, or troubleshoot a VMware Cloud Director + Object Storage Extension lab on AWS EC2 for OSIS development. The lab brings up VCD + OSE on Rocky 9 and wires them against externally-running OSIS/Vault/S3 (typically the engineer's local services, exposed via ngrok). Use when the user says "spin up VCD lab", "create OSE lab", "refresh OSE endpoints", "tear down VCD lab", "VCD lab status", "ssh to VCD lab", or describes a problem with the running lab.
---

<objective>
Orchestrate a VMware Cloud Director + OSE lab for OSIS development. The skill IS the orchestrator: it drives AWS via Terraform, drives the VM via SSH into a shared remote tmux session, and uses the bash scripts under `dev/vcd-ose-lab/scripts/` as the canonical recipes for each phase.

There is no `mage up`. Mage is used for two narrow concerns only:
- `mage preflight` — agent-friendly prerequisite check with one actionable line per failure.
- `mage tfvars` — render `configs/lab.yaml` into `terraform/terraform.tfvars`.

Every other action is driven by this skill following one of the workflows below.
</objective>

<essential_principles>
The lab is **stateful and expensive** (c5.4xlarge, ~$0.85/hr). Apply these rules:

1. **Preflight first, always.** Run `mage preflight` before any action that touches AWS, SSH, or local services. If anything fails, surface the actionable line to the user verbatim. Do not try to fix prerequisites on the user's behalf — ask.

2. **Use the shared remote tmux session `lab`.** All VM commands go through SSH → tmux send-keys to a session named `lab` on the VM. The user can `ssh -t rocky@<ip> 'tmux attach -t lab'` to watch live. Every command is mirrored to `/tmp/lab.log` on the VM. Do not bypass tmux — the user must be able to observe.

3. **Capture state under `_capture/`.** After every meaningful change, update `_capture/session-state.local.md` (gitignored) with the live IDs and URLs. `_capture/SHELL_HISTORY.md` is committed and serves as the ride-along ground truth — do not rewrite it; append if running through new edge cases.

4. **Use FQDN, never the IP, for URLs the browser sees.** VCD's cert SAN includes both, but the Mac keychain trust + CORS allowed-origin only line up reliably with FQDN. The browser hits VCD at `https://<FQDN>/provider` and OSE at `https://<FQDN>:8443`. `/etc/hosts` on the Mac must map FQDN → private IP. Several workflows depend on this — do not silently fall back to the IP.

5. **Surface secrets through `configs/lab.yaml`.** Never bake secrets into commands typed inline. The agent reads `lab.yaml` via the config loader, or instructs the user to fill in fields if missing.

6. **Doc gaps are real.** Fourteen Confluence-doc deltas are documented in `_capture/DOC_GAPS.md`. Read `references/troubleshooting.md` before fighting a symptom — odds are it's a known gotcha (Rocky 9 missing `initscripts`, OSE 3.0 EULA persistence, VCD sites table empty, CORS director URL mismatch, hardcoded `/conf/crypto.yml`).

7. **The lab depends on the engineer's local OSIS/Vault/Cloudserver.** Those run on the Mac, in three named tmux sessions, exposed via ngrok. If they are not running, no lab operation will fully succeed. See `references/local-services.md`.
</essential_principles>

<intake>
Ask the user **only if their intent is ambiguous**. Otherwise route directly.

What do you want to do?

1. **Spin up a fresh lab** — provision the EC2 VM, install VCD + OSE, wire OSIS.
2. **Refresh endpoints** — ngrok URLs changed; re-wire OSE against the new URLs.
3. **Tear down** — `terraform destroy` and clean up local capture.
4. **Status** — run `ose config validate` on the VM; show local + remote health.
5. **SSH to the lab** — attach to the remote `lab` tmux session.
6. **Run preflight** — check prerequisites without touching anything.
7. **Troubleshoot a specific symptom** — load the troubleshooting reference and diagnose.
</intake>

<routing>
| User intent | Workflow |
|---|---|
| "spin up", "create", "bring up", "provision" the lab | `workflows/spin-up.md` |
| "refresh", "ngrok changed", "endpoints changed", "re-wire" | `workflows/refresh-endpoints.md` |
| "tear down", "destroy", "delete the lab", "shut it down" | `workflows/tear-down.md` |
| "status", "is the lab healthy", "what's the state", "config validate" | `workflows/status.md` |
| "ssh", "attach", "open lab terminal" | `workflows/ssh-to-lab.md` |
| "preflight", "check prereqs" | Run `cd dev/vcd-ose-lab && mage preflight`. Read output to user. |
| Specific error message (e.g., "Failed to Start", "CORS error", "NullPointerException CryptoEnv") | Read `references/troubleshooting.md` and match against the symptom list before improvising. |

After reading the workflow, follow it exactly, top to bottom.
</routing>

<reference_index>
**References** (load only what the active workflow tells you to):

- `references/local-services.md` — How to run Vault + Cloudserver + OSIS locally on the Mac in tmux sessions + Redis sentinels + ngrok config. Loaded by spin-up and refresh-endpoints workflows.
- `references/install-sequence.md` — The verified install sequence on the VM, phase by phase, with every interactive prompt and its expected response. Loaded by spin-up.
- `references/troubleshooting.md` — 14 doc gaps as "if you see X, do Y" entries. Loaded on any failure.
- `references/session-state-template.md` — Template for `_capture/session-state.local.md`. Spin-up writes a fresh copy; refresh-endpoints updates the URLs section.

**Scripts** (executed remotely via SCP + bash, or locally on the Mac as noted):

- `scripts/vm/postgres-setup.sh` — initdb, pg_hba rewrite, postgresql.conf patches, start+enable, create roles + DBs, set osedb collation.
- `scripts/vm/install-vmware-vcd-systemd-unit.sh` — write `/etc/systemd/system/vmware-vcd.service`, daemon-reload, enable. Required because the VCD installer skips this on Rocky 9.
- `scripts/vm/fix-vcd-public-urls.sh` — `UPDATE sites SET` to populate `rest_api_endpoint`, `base_ui_endpoint`, `multisite_url`, `name` with the FQDN. Without this, `https://<vm>/provider` returns "Failed to Start".
- `scripts/vm/build-pkcs12-from-vcd-cert.sh` — `openssl pkcs12` with `-passin` and `-name <FQDN>`. The doc's command omits `-passin` (prompts for passphrase) and `-name <IP>` (breaks browser trust later).
- `scripts/vm/wire-osis.sh` — `ose osis admin set` + `ose osis s3 set` + `ose platforms enable` + `ose service restart`. Used by both spin-up and refresh-endpoints.
- `scripts/local/setup-redis-sentinels.sh` — Start three Redis sentinels (26379/80/81) monitoring localhost:6379. Required by OSIS (it hardcodes sentinel mode).
- `scripts/local/build-osis-jar.sh` — Gradle build of OSIS with the right Java 17 path + stub publish creds.
</reference_index>

<success_criteria>
The skill has done its job when, after the active workflow:

- For spin-up: `ose config validate` on the VM shows all seven rows `Valid`, and the user reports the VCD provider UI loads cleanly at the FQDN with no CORS errors clicking into Object Storage.
- For refresh-endpoints: `ose config validate` shows all rows `Valid` again with the new URLs in `_capture/session-state.local.md`.
- For tear-down: `terraform state list` is empty and the AWS console shows the instance and any lab-managed resources gone.
- For status / ssh / preflight: the user has the information they asked for, with no side effects on the lab.

Any failure surfaces a specific next-step instruction to the user, never a stack trace alone.
</success_criteria>
