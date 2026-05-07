# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace
ENV GRADLE_USER_HOME=/root/.gradle

COPY gradlew ./
COPY gradle ./gradle
RUN chmod +x gradlew

COPY settings.gradle.kts build.gradle.kts gradle.properties* ./
COPY app/build.gradle.kts ./app/
COPY shared ./shared
COPY services ./services
COPY tests/build.gradle.kts ./tests/

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon :app:dependencies > /dev/null 2>&1 || true

COPY app ./app
COPY tests ./tests

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon :app:buildFatJar -x test

FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

RUN groupadd --system --gid 1001 foodike \
 && useradd  --system --uid 1001 --gid foodike --home-dir /app --shell /usr/sbin/nologin foodike

COPY --from=build /workspace/app/build/libs/*-all.jar /app/app.jar

USER foodike
EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
