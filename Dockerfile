FROM openjdk:8-alpine

COPY target/uberjar/re-pipe.jar /re-pipe/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/re-pipe/app.jar"]
