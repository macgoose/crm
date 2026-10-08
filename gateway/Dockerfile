### Этап 1: собираем зависимости и приложение
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Сначала копируем POM-файлы, чтобы слой с зависимостями кешировался отдельно.
COPY pom.xml ./
COPY internal-identity/pom.xml internal-identity/
COPY user-service-client/pom.xml user-service-client/
COPY user-service/pom.xml user-service/
COPY gateway/pom.xml gateway/

RUN mvn -B -pl gateway -am dependency:go-offline

# Gateway зависит от двух внутренних библиотек, поэтому собираем их вместе с ним.
COPY internal-identity/src internal-identity/src
COPY user-service-client/src user-service-client/src
COPY gateway/src gateway/src

RUN mvn -B -pl gateway -am clean package -DskipTests


### Этап 2: собираем контейнер с приложением
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/gateway/target/gateway-*.jar app.jar

ARG APP_PORT=8080
ENV SERVER_PORT=${APP_PORT}
EXPOSE ${APP_PORT}

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
