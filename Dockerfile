# syntax=docker/dockerfile:1.7

FROM gradle:8.10-jdk17 AS build
WORKDIR /home/gradle/project

COPY --chown=gradle:gradle settings.gradle.kts build.gradle.kts gradle.properties ./
COPY --chown=gradle:gradle gradle ./gradle
COPY --chown=gradle:gradle app/build.gradle.kts ./app/
COPY --chown=gradle:gradle shared ./shared
COPY --chown=gradle:gradle services ./services
COPY --chown=gradle:gradle tests/build.gradle.kts ./tests/

RUN --mount=type=cache,target=/home/gradle/.gradle \
    gradle --no-daemon :app:dependencies > /dev/null 2>&1 || true

COPY --chown=gradle:gradle app ./app
COPY --chown=gradle:gradle tests ./tests

RUN --mount=type=cache,target=/home/gradle/.gradle \
    gradle --no-daemon :app:buildFatJar -x test

FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

RUN groupadd --system --gid 1001 foodike \
 && useradd  --system --uid 1001 --gid foodike --home-dir /app --shell /usr/sbin/nologin foodike

COPY --from=build /home/gradle/project/app/build/libs/*-all.jar /app/app.jar

USER foodike
EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
