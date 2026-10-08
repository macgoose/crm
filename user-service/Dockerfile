### Этап 1: собираем зависимости и приложение
FROM nxs-test.spimex.dom:8083/spimex-maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Сначала копируем POM-файлы, чтобы слой с зависимостями кешировался отдельно.
COPY pom.xml ./
COPY internal-identity/pom.xml internal-identity/
COPY user-service-client/pom.xml user-service-client/
COPY user-service/pom.xml user-service/
COPY gateway/pom.xml gateway/

RUN mvn -B -pl user-service -am dependency:go-offline

COPY user-service-client/src user-service-client/src
COPY user-service/src user-service/src

RUN mvn -B -pl user-service -am clean package -DskipTests


### Этап 2: собираем контейнер с приложением
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/user-service/target/user-service-*.jar app.jar

ARG APP_PORT=8080
ENV SERVER_PORT=${APP_PORT}
EXPOSE ${APP_PORT}

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
