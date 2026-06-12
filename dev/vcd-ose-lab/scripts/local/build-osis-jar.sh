#!/bin/bash
# Runs on the dev machine (Mac or Linux). Builds the OSIS jar with Java 17 (the
# project requires it; default Java 18 fails with a "consumer needed a runtime
# ... compatible with Java 17" error). Stubs out Sonatype publish creds so the
# upload-artifact script doesn't blow up evaluating the gradle build file.

set -euo pipefail

# shellcheck disable=SC1091
. "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib/platform.sh"

# Default to the repo this script lives in (dev/vcd-ose-lab/scripts/local/build-osis-jar.sh
# → repo root is four levels up). Override via OSIS_REPO if the script is copied
# elsewhere.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OSIS_REPO="${OSIS_REPO:-$(cd "${SCRIPT_DIR}/../../../.." && pwd)}"

JAVA17_HOME="$(detect_java17_home)"
export JAVA_HOME="${JAVA17_HOME}"
export PATH="${JAVA_HOME}/bin:${PATH}"

cd "${OSIS_REPO}"
./gradlew bootJar -x test \
    -PsonatypeUsername=x -PsonatypePassword=x \
    --console=plain

ls -la "${OSIS_REPO}/build/libs/"
