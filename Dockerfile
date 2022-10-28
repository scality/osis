FROM amazoncorretto:17.0.0-alpine AS build-image
EXPOSE 8443
RUN apk add --no-cache bash

COPY /build/libs/osis-scality-*.jar /app/lib/app.jar
ENTRYPOINT ["java","-jar","/app/lib/app.jar","--spring.config.location=file:/conf/application.properties"]
