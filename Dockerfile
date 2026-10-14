# syntax=docker/dockerfile:1
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY hotel-service/pom.xml hotel-service/pom.xml
COPY room-service/pom.xml room-service/pom.xml
COPY booking-service/pom.xml booking-service/pom.xml
COPY checkin-service/pom.xml checkin-service/pom.xml
COPY workflow-service/pom.xml workflow-service/pom.xml

RUN apk add --no-cache maven \
    && mvn -B -q -DskipTests dependency:go-offline || true

COPY common common
COPY hotel-service hotel-service
COPY room-service room-service
COPY booking-service booking-service
COPY checkin-service checkin-service
COPY workflow-service workflow-service

ARG SERVICE_MODULE
RUN mvn -B -pl ${SERVICE_MODULE} -am -DskipTests package \
    && cp ${SERVICE_MODULE}/target/${SERVICE_MODULE}-*.jar /workspace/app.jar

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN apk add --no-cache wget \
    && addgroup -S app && adduser -S app -G app
USER app
COPY --from=build /workspace/app.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
