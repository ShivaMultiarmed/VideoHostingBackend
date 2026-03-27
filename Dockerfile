FROM openjdk:25-ea-jdk AS base
WORKDIR /app
COPY build/libs/video.hosting-2.0.0.jar app.jar

FROM base AS debug
ENTRYPOINT ["sh", "-c", "java --enable-preview -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:${VIDEO_HOSTING_DEBUG_PORT} -jar app.jar"]

FROM base AS release
ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]