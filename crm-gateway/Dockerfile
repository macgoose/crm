### Этап 1: собираем зависимости и приложение
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Сначала копируем POM-файлы, чтобы слой с зависимостями кешировался отдельно.
COPY pom.xml ./
COPY crm-internal-identity/pom.xml crm-internal-identity/
COPY crm-user-service-client/pom.xml crm-user-service-client/
COPY crm-user-service/pom.xml crm-user-service/
COPY crm-gateway/pom.xml crm-gateway/

RUN mvn -B -pl crm-gateway -am dependency:go-offline

# Gateway зависит от двух внутренних библиотек, поэтому собираем их вместе с ним.
COPY crm-internal-identity/src crm-internal-identity/src
COPY crm-user-service-client/src crm-user-service-client/src
COPY crm-gateway/src crm-gateway/src

RUN mvn -B -pl crm-gateway -am clean package -DskipTests


### Этап 2: собираем контейнер с приложением
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/crm-gateway/target/crm-gateway-*.jar app.jar

ARG APP_PORT=8080
ENV SERVER_PORT=${APP_PORT}
EXPOSE ${APP_PORT}

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
