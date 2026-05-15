# Stage 1: Build fat JAR
FROM gradle:8.14-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle :server:buildFatJar --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
COPY --from=build /app/server/build/libs/shoppinglist-server.jar /app/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/shoppinglist-server.jar"]
