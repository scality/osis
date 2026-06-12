#!/bin/bash
# Runs on the VM as root. Wires OSE to OSIS + S3 endpoints.
#
# Required env vars:
#   OSIS_URL              public URL of the OSIS instance
#   S3_URL                public URL of the S3 (Cloudserver) instance
#   OSIS_ACCESS_KEY       OSIS super-admin access key
#   OSIS_SECRET_KEY       OSIS super-admin secret key
#
# Secrets are passed via env so they don't appear in argv / /proc/<pid>/cmdline.
# `ose osis admin set` is interactive (asks for the secret, then a y/N for the
# password-complexity warning). We feed both via stdin.

set -euo pipefail

: "${OSIS_URL:?OSIS_URL must be set}"
: "${S3_URL:?S3_URL must be set}"
: "${OSIS_ACCESS_KEY:?OSIS_ACCESS_KEY must be set}"
: "${OSIS_SECRET_KEY:?OSIS_SECRET_KEY must be set}"

printf '%s\ny\n' "${OSIS_SECRET_KEY}" | \
    ose osis admin set --name scality --url "${OSIS_URL}" --user "${OSIS_ACCESS_KEY}"

ose osis s3 set --name scality --url "${S3_URL}"
ose platforms enable osis --name scality
ose service restart

echo '--- validating ---'
ose config validate
