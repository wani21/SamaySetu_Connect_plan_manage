FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY samaysetu-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENV SERVER_PORT=8080
ENTRYPOINT ["java", "-jar", "app.jar"]
