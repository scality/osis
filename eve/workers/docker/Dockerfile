FROM ubuntu:bionic

VOLUME /workdir

ARG BUILDBOT_VERSION=0.9.12

VOLUME /home/eve/workspace

WORKDIR /home/eve/workspace

RUN apt-get update && apt-get install -y \
    openjdk-8-jdk \
    python3-buildbot-worker \
    curl \
    unzip \
    git

CMD buildbot-worker create-worker . ${BUILDMASTER}:${BUILDMASTER_PORT} ${WORKERNAME} ${WORKERPASS} && buildbot-worker start --nodaemon
