FROM openjdk:17-jdk-slim
WORKDIR /app
COPY build/libs/video.hosting-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5555", "-jar", "app.jar"]