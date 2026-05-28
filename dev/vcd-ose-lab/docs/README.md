# VCD-OSE Lab

Brings up a VMware Cloud Director + Object Storage Extension lab on a Rocky 9 EC2 VM and wires it to externally-running Scality OSIS/Vault/S3 endpoints. Intended for OSIS development and integration testing.

Source doc (Scality-internal): Confluence page id `2138571097`, "Creating a VMware Cloud Director Object Storage Extension Lab on Rocky 8".

## Prerequisites

- macOS or Linux with: `go` (1.21+), `mage`, `terraform`, `ssh`, `scp`.
- AWS access to one of: `eu-north-1`, `us-west-2`, `ap-northeast-1`. Configured via `aws sso login` (or static creds).
- Scality VPN connection.
- VMware binaries placed in `binaries/` (see `binaries/README.md`).
- `configs/lab.yaml` populated from `configs/lab.example.yaml`.
- OSIS/Vault/S3 running and reachable from EC2 (e.g., via ngrok), URLs set in `configs/lab.yaml`.

## Usage

```
mage preflight    # verify prerequisites
mage tfvars       # render configs/lab.yaml into terraform/terraform.tfvars
mage up           # provision VM, install VCD+OSE, wire endpoints (~25-35 min)
mage status       # `ose config validate` on the VM
mage refresh      # re-wire endpoints after editing configs/lab.yaml
mage ssh          # attach to the shared tmux session on the VM
mage down         # destroy AWS resources
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

Invoke the OSIS skill `vcd-ose-lab` (at `.claude/skills/vcd-ose-lab/SKILL.md`). The skill routes intents to the targets above and surfaces preflight failures as human-actionable instructions.

## After `mage up` succeeds

Trust the VCD certificate on your Mac so the provider UI loads without warnings:

```
scp -i <key> root@<eth0_ip>:/opt/vmware/vcloud-director/cert.pem ~/Downloads/vcd-lab.pem
sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain ~/Downloads/vcd-lab.pem
```

Then visit `https://<eth0_ip>/provider` (requires Scality VPN).
