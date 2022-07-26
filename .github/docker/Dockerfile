FROM ubuntu:bionic

VOLUME /workdir

ARG GRADLE_VERSION=6.7.1

ENV PATH=${PATH}:/opt/gradle/gradle-${GRADLE_VERSION}/bin

RUN apt-get update && apt-get install -y \
    openjdk-8-jdk \
    python3-buildbot-worker \
    curl \
    unzip \
    git

RUN curl -o gradle-${GRADLE_VERSION}-bin.zip https://downloads.gradle-dn.com/distributions/gradle-${GRADLE_VERSION}-bin.zip && \
    mkdir /opt/gradle && \
    unzip -d /opt/gradle gradle-${GRADLE_VERSION}-bin.zip

WORKDIR /workdir

CMD buildbot-worker create-worker . "$BUILDMASTER:$BUILDMASTER_PORT" "$WORKERNAME" "$WORKERPASS" \
    && buildbot-worker start --nodaemon
