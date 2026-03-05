FROM openjdk:17-jdk-slim AS base
WORKDIR /app
COPY build/libs/video.hosting-2.0.0.jar app.jar

FROM base AS debug
ENTRYPOINT ["sh", "-c", "java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:${VIDEO_HOSTING_DEBUG_PORT} -jar app.jar"]

FROM base AS release
ENTRYPOINT ["java", "-jar", "app.jar"]