# Stage 1: Build
FROM maven:latest AS build
WORKDIR /app
COPY . .
RUN mvn clean install -DskipTests

## Stage 2: Run

FROM openjdk:21
ARG JAR_FILE=target/*.jar
COPY ./target/aidbBackend-*.jar app.jar
EXPOSE 9000
ENV SPRING_DEVTOOLS_RESTART_ENABLED=true
ENV SPRING_DEVTOOLS_LIVE_RELOAD_ENABLED=true
ENTRYPOINT ["java","-jar","/app.jar"]