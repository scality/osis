#!/bin/bash
# Runs on the VM as root, after cell-management-tool system-setup.
# system-setup leaves the sites table with empty rest_api_endpoint /
# base_ui_endpoint / multisite_url, which causes the H5 UI to throw
# "Failed to Start" at /provider. Populate them with the FQDN URL, then
# restart vmware-vcd so the H5 UI picks the values up.
#
# Doc gap #9.

set -euo pipefail

FQDN=$(hostname -f)
echo "Setting VCD public endpoints to https://${FQDN}"

# Pass the FQDN via psql -v so it is quoted server-side instead of
# shell-interpolated into the SQL.
result="$(sudo -u postgres psql -d vcddb -v ON_ERROR_STOP=1 -v fqdn="${FQDN}" <<'SQL'
UPDATE sites SET
    rest_api_endpoint = 'https://' || :'fqdn',
    multisite_url     = 'https://' || :'fqdn',
    base_ui_endpoint  = 'https://' || :'fqdn',
    name              = 'VCD'
WHERE is_local_site = true
RETURNING name, rest_api_endpoint, base_ui_endpoint;
SQL
)"
printf '%s\n' "${result}"

# UPDATE 0 exits 0 — assert the local-site row actually existed, or the
# "Failed to Start" condition this script exists to fix persists silently.
if ! grep -q '^UPDATE 1' <<< "${result}"; then
    echo "ERROR: no local site row updated — run this after cell-management-tool system-setup." >&2
    exit 1
fi

echo 'Restarting vmware-vcd...'
systemctl restart vmware-vcd

echo 'Waiting for port 443 to come back...'
for _ in {1..60}; do
    sleep 5
    if ss -tln | grep -q ':443 '; then
        echo 'VCD listening on 443.'
        exit 0
    fi
done

echo 'WARNING: vmware-vcd did not start listening on 443 within ~5 min.' >&2
exit 1
