# VCD-OSE Lab

Brings up a VMware Cloud Director + Object Storage Extension lab on a Rocky 9 EC2 VM and wires it to externally-running Scality OSIS/Vault/S3 endpoints. Intended for OSIS development and integration testing.

Source doc (Scality-internal): Confluence page id `2138571097`, "Creating a VMware Cloud Director Object Storage Extension Lab on Rocky 8".

## Prerequisites

- A dev machine (Mac or Linux — Debian/Ubuntu or Fedora/Rocky/RHEL) with: `go` (1.21+), `mage`, `terraform`, `ssh`, `scp`, `tmux`, `redis`, JDK 17, `ngrok`, `jq`.
  - **Mac:** `brew install go mage terraform tmux redis openjdk@17 ngrok jq`
  - **Debian/Ubuntu:** `sudo apt install golang-go tmux redis-server openjdk-17-jdk jq`; install terraform via the HashiCorp apt repo and ngrok via https://ngrok.com/download `.deb`; install mage with `go install github.com/magefile/mage@latest`.
  - **Fedora/Rocky/RHEL:** `sudo dnf install golang tmux redis java-17-openjdk-devel jq`; `sudo dnf config-manager --add-repo https://rpm.releases.hashicorp.com/RHEL/hashicorp.repo && sudo dnf install terraform`; ngrok via https://ngrok.com/download `.rpm`; mage with `go install github.com/magefile/mage@latest`.
- AWS access to one of: `eu-north-1`, `us-west-2`, `ap-northeast-1`. Configured via `aws sso login` (or static creds).
- Scality VPN connection.
- VMware binaries placed in `binaries/` (see `binaries/README.md`).
- `configs/lab.yaml` populated from `configs/lab.example.yaml`.
- OSIS/Vault/S3 running and reachable from EC2 (e.g., via ngrok), URLs set in `configs/lab.yaml`.

## Usage

Mage covers the two pieces that are usefully wrapped in Go (typed config + structured preflight checks). Everything else — provisioning, install, wiring, teardown — is driven by the `vcd-ose-lab` Claude Code skill at `.claude/skills/vcd-ose-lab/`. The skill is the orchestrator; it calls `terraform` and `ssh` directly via Bash following the workflows in `.claude/skills/vcd-ose-lab/workflows/`.

```
mage preflight    # verify prerequisites (with one actionable line per failure)
mage tfvars       # render configs/lab.yaml into terraform/terraform.tfvars
```

A passing `mage preflight` looks like this:

```
[OK] config file
[OK] config fields
[OK] terraform on PATH
[OK] aws on PATH
[OK] ssh on PATH
[OK] scp on PATH
[OK] AWS credentials (profile sso)
[OK] SSH key
[OK] VCD installer (.bin)  (vmware-vcloud-director-distribution-10.5.1-23401219.bin)
[OK] OSE package (.rpm)  (vmware-ose-3.0.0-23443325.el8.x86_64.rpm)
```

Any `[FAIL]` row prints an actionable next step (install a tool, fill a config field, `aws sso login`, etc). Fix each and re-run.

## Agent usage

Invoke the OSIS skill `vcd-ose-lab` (at `.claude/skills/vcd-ose-lab/SKILL.md`). Say one of: "spin up VCD lab", "tear down VCD lab", "refresh OSE endpoints", "VCD lab status", "ssh to VCD lab". The skill routes to the matching workflow and drives terraform/ssh from there. Preflight failures surface as human-actionable instructions.

## After the lab is up

Trust the VCD certificate on your dev machine so the provider UI loads without warnings:

```bash
scp -i <key> rocky@<eth0_ip>:/opt/vmware/vcloud-director/cert.pem ~/Downloads/vcd-lab.pem
```

Then **one** of (sourcing `scripts/local/lib/platform.sh` lets `ca_trust_commands` pick the right one for you):

- **Mac:** `sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain ~/Downloads/vcd-lab.pem`
- **Debian/Ubuntu:** `sudo cp ~/Downloads/vcd-lab.pem /usr/local/share/ca-certificates/vcd-lab.crt && sudo update-ca-certificates`
- **Fedora/Rocky/RHEL:** `sudo cp ~/Downloads/vcd-lab.pem /etc/pki/ca-trust/source/anchors/vcd-lab.pem && sudo update-ca-trust`

Add the FQDN ↔ private IP mapping to your `/etc/hosts` (same path on Mac and Linux). Then visit `https://<eth0-FQDN>/provider` (requires Scality VPN).

Firefox on Linux uses its own NSS trust store — import the PEM under Settings → Privacy & Security → View Certificates → Import if you use Firefox. Chrome/Chromium read the system NSS DB and are fine after `update-ca-trust` / `update-ca-certificates`.
