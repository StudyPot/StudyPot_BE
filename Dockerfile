# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x ./gradlew

COPY src ./src
RUN ./gradlew --no-daemon clean bootJar \
	&& find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -exec cp {} /workspace/app.jar \;

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S studypot && adduser -S studypot -G studypot

COPY --from=build /workspace/app.jar /app/app.jar

ENV JAVA_TOOL_OPTIONS="-XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

USER studypot

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
