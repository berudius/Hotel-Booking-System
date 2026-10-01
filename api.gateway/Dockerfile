# Етап 1: Збірка
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY common ./common
RUN cd common && mvn clean install

COPY api.gateway ./api.gateway
RUN cd api.gateway && mvn clean package -DskipTests

# Етап 2: Запуск
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/api.gateway/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]