#!/bin/bash
# Runs on the VM as root. Wires OSE to OSIS + S3 endpoints.
#
# Usage: wire-osis.sh <osis_url> <s3_url> <access_key> <secret_key>
#
# `ose osis admin set` is interactive (asks for the secret, then a y/N for the
# password-complexity warning). This script uses `expect`-style input
# handling via the OSE-friendly `printf | ose ...` pattern, which works for
# admin set in OSE 3.0.

set -euo pipefail

OSIS_URL="${1:?missing osis url}"
S3_URL="${2:?missing s3 url}"
ACCESS_KEY="${3:?missing access key}"
SECRET_KEY="${4:?missing secret key}"

printf '%s\ny\n' "${SECRET_KEY}" | \
    ose osis admin set --name scality --url "${OSIS_URL}" --user "${ACCESS_KEY}"

ose osis s3 set --name scality --url "${S3_URL}"
ose platforms enable osis --name scality
ose service restart

echo '--- validating ---'
ose config validate
