---
name: vcd-ose-lab
description: Spin up, refresh, or tear down a VMware Cloud Director + OSE lab on AWS EC2 for OSIS development. Triggers on "spin up VCD lab", "create OSE lab", "refresh OSE endpoints", "tear down VCD lab", "VCD lab status", "ssh to VCD lab".
---

# VCD-OSE Lab

Brings up a Rocky 9 EC2 VM running VMware Cloud Director + Object Storage Extension, wired against OSIS/Vault/S3 endpoints the user already has running (typically exposed via ngrok). Code lives at `dev/vcd-ose-lab/`; this skill is its agent-facing surface.

## Intent → command

| User intent | Run |
|---|---|
| "spin up / create VCD lab" | `cd dev/vcd-ose-lab && mage up` |
| "tear down VCD lab" | `cd dev/vcd-ose-lab && mage down` |
| "refresh OSE endpoints" / "ngrok URLs changed" | `cd dev/vcd-ose-lab && mage refresh` |
| "VCD lab status" / "is the lab healthy" | `cd dev/vcd-ose-lab && mage status` |
| "ssh to VCD lab" / "attach to the lab session" | `cd dev/vcd-ose-lab && mage ssh` |
| "check VCD lab prerequisites" | `cd dev/vcd-ose-lab && mage preflight` |

## How to handle preflight failures

`mage up` runs `mage preflight` first. If preflight fails it prints one line per missing prerequisite, each with an actionable next step. Surface those to the user verbatim — they describe exactly what the user needs to do:

- AWS credentials missing → "Please run `aws sso login --profile <profile>` and retry."
- Terraform not installed → "Please run `brew install terraform`."
- VMware binaries missing → "Please download `<filename>` from Scality Drive (see Confluence id 2138571097) and place it in `dev/vcd-ose-lab/binaries/`."
- `configs/lab.yaml` missing or incomplete → "Copy `configs/lab.example.yaml` to `configs/lab.yaml` and fill in the listed fields."
- Scality VPN unreachable → "Please connect to Scality VPN and retry."

Do not attempt to fix these on the user's behalf. Ask, wait, retry.

## Shared session

Once the VM is up, all install commands run via SSH are mirrored to a `tmux` session named `lab` on the VM. The user can attach via `mage ssh` to watch in real time or intervene.

## After success

`mage up` prints the eth0 IP and a snippet for trusting the VCD certificate on the user's Mac. Pass that snippet through to the user; cert trust is intentionally manual (requires sudo on the user's machine).

## References

- Source documentation: Scality Confluence page id `2138571097`.
- Implementation: `dev/vcd-ose-lab/` (Go + Mage + Terraform + embedded bash).
- Capture artifacts and troubleshooting: `dev/vcd-ose-lab/_capture/`.
