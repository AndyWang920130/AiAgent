# Docker Deployment

Run commands from the project root.

This deployment uses one shared `docker-compose.yaml`. Environment differences are controlled by env files:

- `env.prod`: Spring profile `prod`, database `twsny_prod`, MySQL host port `16011`, nginx port `80`
- `env.test`: Spring profile `test`, database `twsny_test`, MySQL host port `16012`, nginx port `8081`

Prod and test are fully isolated at the Docker layer. Each environment gets its own MySQL container, Docker volume, database name, backend container, nginx container, and host ports.

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

For test, use `APP_ENV=test`, `SPRING_PROFILE=test`, `MYSQL_DATABASE=twsny_test`, `MYSQL_PORT=16012`, `HTTP_PORT=8081`, and `COMPOSE_PROJECT_NAME=myapp-test`.

## Running prod and test at the same time

Prod and test can run at the same time from the same Compose file because their env files select different Docker resources:

- Prod MySQL container: `myapp-mysql-prod`
- Test MySQL container: `myapp-mysql-test`
- Prod MySQL volume: `myapp-mysql-data-prod`
- Test MySQL volume: `myapp-mysql-data-test`
- Prod database: `twsny_prod`
- Test database: `twsny_test`
- Prod MySQL host port: `16011`
- Test MySQL host port: `16012`

## MySQL initialization

The MySQL image creates the database named by `MYSQL_DATABASE`. The schema and seed data come from `back/myapp/src/main/resources/db/mysql/initial.sql` and are applied only to that environment's database on first volume creation.

MySQL only runs initialization scripts when its data volume is first created. If you already started an older deployment and want the new isolation, stop the affected stack and migrate or remove the old volume intentionally before starting again.
