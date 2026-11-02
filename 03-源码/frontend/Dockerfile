FROM node:24.22.2-alpine AS build
WORKDIR /build
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build
FROM nginx:stable-alpine
COPY --from=build /build/dist /usr/share/nginx/html
ENV NGINX_CLIENT_MAX_BODY_SIZE=101m
COPY nginx.conf /etc/nginx/templates/default.conf.template
EXPOSE 80
