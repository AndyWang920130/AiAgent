FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /app

COPY back/myapp/pom.xml ./
RUN mvn -B dependency:go-offline

COPY back/myapp/src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN addgroup --system spring && adduser --system spring --ingroup spring
COPY --from=build /app/target/*.jar app.jar

USER spring:spring
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
