# syntax=docker/dockerfile:1

# Etapa 1: instala dependencias e gera o build de producao (arquivos estaticos).
FROM node:22-alpine AS build

WORKDIR /workspace

# Copiar o package.json antes do codigo permite reutilizar o cache do npm ci.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# Etapa 2: imagem final, contendo somente o Nginx e os arquivos estaticos.
FROM nginx:1.27-alpine AS runtime

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80
