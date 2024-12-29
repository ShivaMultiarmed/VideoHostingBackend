FROM openjdk:17-jdk-slim
WORKDIR /app
COPY out/artifacts/video_hosting_main_jar/video.hosting.main.jar app.jar
ENTRYPOINT ["java", "-cp", "app.jar", "mikhail.shell.video.hosting.ApplicationKt"]
