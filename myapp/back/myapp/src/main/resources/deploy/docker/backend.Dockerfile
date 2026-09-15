FROM maven:3.9.8-eclipse-temurin-17-alpine AS build
WORKDIR /app
# Maven 配置
COPY back/myapp/src/main/resources/deploy/docker/maven-settings.xml /root/.m2/settings.xml
# 先复制项目 pom.xml
COPY back/myapp/pom.xml ./
# 复制自定义 jar
COPY back/myapp/src/main/resources/deploy/docker/opt/lib/excel-spring-boot-starter-1.0.0.jar /tmp/
# 安装到 Docker 容器自己的 Maven 本地仓库
RUN mvn install:install-file \
    -Dfile=/tmp/excel-spring-boot-starter-1.0.0.jar \
    -DgroupId=cn.twsny \
    -DartifactId=excel-spring-boot-starter \
    -Dversion=1.0.0 \
    -Dpackaging=jar

# 下载项目依赖
RUN mvn -B dependency:go-offline

# 再复制源码
COPY back/myapp/src ./src
# 打包
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
