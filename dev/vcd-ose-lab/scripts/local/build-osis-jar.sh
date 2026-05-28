#!/bin/bash
# Runs on the Mac. Builds the OSIS jar with Java 17 (the project requires it;
# default Java 18 fails with a "consumer needed a runtime ... compatible with
# Java 17" error). Stubs out Sonatype publish creds so the upload-artifact
# script doesn't blow up evaluating the gradle build file.

set -euo pipefail

OSIS_REPO="${OSIS_REPO:-${HOME}/anurag-builds-things/scality/osis}"

JAVA17_HOME="$(/opt/homebrew/bin/brew --prefix openjdk@17)/libexec/openjdk.jdk/Contents/Home"
if [ ! -d "${JAVA17_HOME}" ]; then
    echo "openjdk@17 not found. Run: brew install openjdk@17" >&2
    exit 1
fi

export JAVA_HOME="${JAVA17_HOME}"
export PATH="${JAVA_HOME}/bin:${PATH}"

cd "${OSIS_REPO}"
./gradlew bootJar -x test \
    -PsonatypeUsername=x -PsonatypePassword=x \
    --console=plain

ls -la "${OSIS_REPO}/build/libs/"
