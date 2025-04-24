## Stage 1: Build
#FROM maven:latest AS build
#WORKDIR /app
#COPY . .
#RUN mvn clean install -DskipTests
#
### Stage 2: Run
#
#FROM openjdk:21
#ARG JAR_FILE=target/*.jar
#COPY ./target/aidbBackend-*.jar app.jar
#EXPOSE 9000
#ENV SPRING_DEVTOOLS_RESTART_ENABLED=true
#ENV SPRING_DEVTOOLS_LIVE_RELOAD_ENABLED=true
#ENTRYPOINT ["java","-jar","/app.jar"]

# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
