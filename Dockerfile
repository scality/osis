FROM gradle:6.7.1-jdk8-openj9 AS gradle-build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle bootJar

FROM amazoncorretto:8-alpine-jdk AS build-image
EXPOSE 8443
RUN apk add --no-cache bash
COPY --from=gradle-build /home/gradle/src/build/libs/osis-scality-*.jar /app/lib/app.jar
ENTRYPOINT ["java","-jar","/app/lib/app.jar","--spring.config.location=file:/conf/application.properties"]
