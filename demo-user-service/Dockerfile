# Етап 1: Збірка
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY common ./common
RUN cd common && mvn clean install

COPY demo-user-service ./demo-user-service
RUN cd demo-user-service && mvn clean package -DskipTests

# Етап 2: Запуск
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/demo-user-service/target/*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]