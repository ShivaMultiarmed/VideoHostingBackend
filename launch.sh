cd /home/mikhail_shell/Hosting/Application
docker-compose up -d --force-recreate
nohup java -jar build/libs/video.hosting-0.0.1-SNAPSHOT.jar > /dev/null &
