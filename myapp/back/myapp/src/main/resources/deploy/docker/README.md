# Deployment

Run these commands from the project root on the server.

```bash
docker compose -f back/myapp/src/main/resources/deploy/docker/docker-compose.yaml up -d --build
```

Check service status and logs:

```bash
docker compose -f back/myapp/src/main/resources/deploy/docker/docker-compose.yaml ps
docker compose -f back/myapp/src/main/resources/deploy/docker/docker-compose.yaml logs -f
```

Optional environment overrides can be copied from `back/myapp/src/main/resources/deploy/docker/.env.example` to `back/myapp/src/main/resources/deploy/docker/.env`:

```env
MYSQL_ROOT_PASSWORD=change-this-password
MYSQL_PORT=16011
HTTP_PORT=80
APP_CORS_ALLOWED_ORIGINS=http://your-domain.com,http://www.your-domain.com
```

When using the deployment `.env`, run Compose with:

```bash
docker compose --env-file back/myapp/src/main/resources/deploy/docker/.env -f back/myapp/src/main/resources/deploy/docker/docker-compose.yaml up -d --build
```

The nginx gateway listens on `HTTP_PORT` and proxies `/api/` requests to the backend service.
