# Build stage: compile and package the app
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline -DskipTests
COPY src ./src
RUN mvn -B -DskipTests clean package

# Run stage: only the built WAR and a JRE
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/movie_ticket_booking_system_backend.war app.war
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8081} -jar app.war"]
