FROM openjdk:17-jdk-slim
WORKDIR /app
COPY build/libs/video.hosting-1.0.0.jar app.jar
ENTRYPOINT ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:${HOSTING_DEBUG_PORT}", "-jar", "app.jar"]