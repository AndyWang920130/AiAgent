# Docker Deployment

Run commands from the project root.

This deployment uses one shared `docker-compose.yaml`. Environment differences are controlled by env files:

- `env.prod`: Spring profile `prod`, database `twsny_prod`, nginx port `80`
- `env.test`: Spring profile `test`, database `twsny_test`, nginx port `8081`

MySQL is initialized with both databases by `mysql-init.sql`.


## China mirror acceleration

The deploy Dockerfiles already configure these build-time mirrors:

- Maven: `https://maven.aliyun.com/repository/central` via `maven-settings.xml`
- npm: `https://registry.npmmirror.com`
- Alpine packages: `https://mirrors.aliyun.com/alpine`

Base image pulls such as `mysql:8.0`, `node:22-alpine`, `nginx:1.27-alpine`, and `maven:3.9.8-eclipse-temurin-17-alpine` are controlled by the Docker daemon, not by the Dockerfile. On a China server, configure Docker registry mirrors in `/etc/docker/daemon.json`, for example:

```json
{
  "registry-mirrors": [
    "https://docker.m.daocloud.io",
    "https://docker.1ms.run"
  ]
}
```

Then restart Docker:

```bash
sudo systemctl daemon-reload
sudo systemctl restart docker
```
## Start prod

```bash
docker compose --env-file back/myapp/src/main/resources/deploy/docker/env.prod \
  -f back/myapp/src/main/resources/deploy/docker/docker-compose.yaml \
  up -d --build
```

## Start test

```bash
docker compose --env-file back/myapp/src/main/resources/deploy/docker/env.test \
  -f back/myapp/src/main/resources/deploy/docker/docker-compose.yaml \
  up -d --build
```

## Check status and logs

Use the same env file you used for startup:

```bash
docker compose --env-file back/myapp/src/main/resources/deploy/docker/env.prod \
  -f back/myapp/src/main/resources/deploy/docker/docker-compose.yaml ps

docker compose --env-file back/myapp/src/main/resources/deploy/docker/env.prod \
  -f back/myapp/src/main/resources/deploy/docker/docker-compose.yaml logs -f
```

## Environment variables

Important variables in `env.prod` and `env.test`:

```env
APP_ENV=prod
SPRING_PROFILE=prod
MYSQL_DATABASE=twsny_prod
MYSQL_ROOT_PASSWORD=123456
MYSQL_PORT=16011
HTTP_PORT=80
APP_CORS_ALLOWED_ORIGINS=http://localhost,http://localhost:80
COMPOSE_PROJECT_NAME=myapp-prod
```

## Running prod and test at the same time

The single Compose file can deploy either environment by changing `--env-file`.

Running both at the same time with one shared MySQL container needs extra orchestration because two Compose projects cannot both own the same `mysql` service/container. The usual options are:

- Run only one app environment per server with this file.
- Run both app environments in one Compose project using separate service names.
- Make MySQL an external/shared service and deploy prod/test app stacks separately against it.

## MySQL initialization

`mysql-init.sql` creates and seeds both databases: `twsny_prod` and `twsny_test`.

MySQL only runs initialization scripts when the `mysql-data` volume is first created. If you already started the old deployment and the volume exists, create the missing database manually or remove the volume before starting again.

