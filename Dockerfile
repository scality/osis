FROM gradle:7.3.0-jdk17-alpine AS gradle-build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle bootJar -PsonatypeUsername=scality.com -PsonatypePassword=Artesca12345!

FROM amazoncorretto:17.0.0-alpine AS build-image
EXPOSE 8443
RUN apk add --no-cache bash
COPY --from=gradle-build /home/gradle/src/build/libs/osis-scality-*.jar /app/lib/app.jar
ENTRYPOINT ["java","-jar","/app/lib/app.jar","--spring.config.location=file:/conf/application.properties"]
