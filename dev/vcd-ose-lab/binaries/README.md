# Binaries

VMware binaries are not redistributable. Download them from Scality's internal Drive and place them here:

- `vmware-vcloud-director-distribution-<version>.bin`
- `vmware-ose-<version>.el7.x86_64.rpm`

Drive links are tracked in the Scality-internal Confluence page (id `2138571097`).

`mage preflight` verifies these files exist (and optionally checks MD5 from `configs/lab.yaml`) before running `mage up`.
