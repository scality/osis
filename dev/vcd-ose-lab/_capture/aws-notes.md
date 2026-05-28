# AWS provisioning notes

First successful `terraform apply` on 2026-05-28.

## Resolved values

| Field | Value |
|---|---|
| Account | 944690102204 |
| Region | eu-north-1 |
| VPC | vpc-0703e4f30bb09415d (scality-engineering-vpc) |
| Subnet | subnet-027470be720a7fd4a (scality-engineering-vpc-private-eu-north-1a) |
| Security groups | sg-020e7ef5404993cc4 (allow-vpn-private-and-vms) + sg-0eb2b55eadddd4087 (pww_anurag_security_group) |
| AMI | ami-0159af92e963cb183 (Rocky Linux 9.7 Blue Onyx; kernel 5.14.0-611.5.1.el9_7) |
| Instance type | c5.4xlarge |
| Root volume | 400 GB gp2 |
| Key pair | anuragM (matches ~/.ssh/id_rsa, fingerprint 9d:90:2a:bd:e3:e6:d9:2c:26:97:06:e0:62:e2:49:93) |
| SSH user | rocky |

## Timings

- `terraform apply`: 13 seconds.
- SSH became reachable: ~55 seconds after apply finished.

## Notes

- `data.aws_ami.rocky` filter (`owner=792107900819`, name `Rocky-9-EC2-Base-*`) resolved the right AMI without override.
- No public IP; reach is via Scality VPN to the private subnet IP.
- Default Rocky 9 OpenSSH (10.x) prints a `post-quantum key exchange` warning. Cosmetic; no action.
