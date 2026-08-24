FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY services/api/target/*.jar api.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "api.jar"]
