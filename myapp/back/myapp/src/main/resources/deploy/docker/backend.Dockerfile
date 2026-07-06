FROM maven:3.9.8-eclipse-temurin-17-alpine AS build
WORKDIR /app

COPY back/myapp/src/main/resources/deploy/docker/maven-settings.xml /root/.m2/settings.xml
COPY back/myapp/pom.xml ./
RUN mvn -B dependency:go-offline

COPY back/myapp/src ./src
RUN mvn -B clean package -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories \
    && apk add --no-cache curl

RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /app/target/*.jar app.jar

USER spring:spring
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
