FROM node:24-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
ARG PUBLIC_API_URL=
ENV PUBLIC_API_URL=${PUBLIC_API_URL}
ARG PUBLIC_PAGOS_SIMULADOS=false
ENV PUBLIC_PAGOS_SIMULADOS=${PUBLIC_PAGOS_SIMULADOS}
RUN npm run build

FROM nginx:1.28-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
