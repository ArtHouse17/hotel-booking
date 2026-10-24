FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /project
COPY . .
RUN mvn -B -ntp verify

FROM eclipse-temurin:17-jre
ARG SERVICE
WORKDIR /app
COPY --from=build /project/${SERVICE}/target/${SERVICE}-1.0.0.jar app.jar
ENV SERVER_ADDRESS=0.0.0.0
ENTRYPOINT ["java", "-jar", "app.jar"]
