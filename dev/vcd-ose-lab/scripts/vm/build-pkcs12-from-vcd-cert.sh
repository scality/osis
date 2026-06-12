#!/bin/bash
# Runs on the VM as root. Builds a PKCS#12 from the VCD-generated cert.pem /
# cert.key and imports it as OSE's server keystore.
#
# CERT_PASSPHRASE must be supplied via env (no default). It's passed to
# openssl via `env:` so it never appears in argv / /proc/<pid>/cmdline.
#
# Two fixes vs the Confluence doc:
#   - The doc omits `-passin`, so openssl prompts for the input key passphrase
#     and the script hangs (doc gap #10).
#   - The doc uses `-name <IP>`. Using the FQDN as friendly-name keeps the
#     keystore alias aligned with the cert CN, which keeps browser trust
#     consistent once the OSE endpoint URL is switched to the FQDN (doc gap #13).

set -euo pipefail

: "${CERT_PASSPHRASE:?CERT_PASSPHRASE must be set in the environment}"

FQDN=$(hostname -f)

# openssl reads `env:VAR` for both -passin and -passout. The value is taken
# from the environment, not the command line, so it stays out of process
# listings.
export CERT_PASSPHRASE

cd /root
openssl pkcs12 -export -out cert-fqdn.p12 \
    -inkey /opt/vmware/vcloud-director/cert.key \
    -in /opt/vmware/vcloud-director/cert.pem \
    -passin env:CERT_PASSPHRASE \
    -password env:CERT_PASSPHRASE \
    -name "${FQDN}"

ls -la /root/cert-fqdn.p12

# ose cert import still takes --secret on argv. Pass via stdin if/when
# the OSE CLI grows a stdin option; for now this is unavoidable.
ose cert import --path /root/cert-fqdn.p12 --secret "${CERT_PASSPHRASE}" --force
