FROM node:22-alpine AS build
WORKDIR /app

COPY front/package*.json ./
RUN npm config set registry https://registry.npmmirror.com
RUN npm ci

COPY front ./
RUN npm run build:fast

FROM nginx:1.27-alpine
COPY back/myapp/src/main/resources/deploy/docker/nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
