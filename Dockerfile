FROM openjdk:8-jdk-alpine
EXPOSE 8443
VOLUME /workdir
ARG DEPENDENCY=build
RUN echo ${DEPENDENCY}
COPY ${DEPENDENCY}/libs/osis.scality-0.1.0-SNAPSHOT.jar /app/lib/app.jar
ENTRYPOINT ["java","-jar","/app/lib/app.jar"]