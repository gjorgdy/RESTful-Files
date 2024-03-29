#
# Build stage
#
FROM gradle:8.5.0-jdk21-alpine AS build
WORKDIR /usr/app
COPY build.gradle .
COPY settings.gradle .
COPY src/main ./src/main
RUN gradle clean bootJar


#
# Package stage
#
FROM amazoncorretto:21-al2023-headless
#21-alpine3.18
ARG JAR_FILE=/usr/app/build/libs/*.jar
COPY --from=build $JAR_FILE /app/runner.jar
RUN touch .env
#RUN apk add --no-cache msttcorefonts-installer fontconfig
ENTRYPOINT java -jar /app/runner.jar