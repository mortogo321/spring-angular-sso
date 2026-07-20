# syntax=docker/dockerfile:1

# Stage 1: build the Angular production bundle with bun.
# Node 26 base + the bun binary: bun is the package manager/script runner,
# but the Angular CLI checks process.version and rejects bun's emulated Node
# (v24.3.0 < required v24.15.0), so `ng` itself must run on real Node.
# oven/bun:1.3.14-alpine matches the bun version pinned in this repo.
FROM node:26-alpine AS build
COPY --from=oven/bun:1.3.14-alpine /usr/local/bin/bun /usr/local/bin/bun

WORKDIR /app

COPY package.json bun.lock ./
RUN bun install --frozen-lockfile

COPY . .
RUN bun run ng build

# Stage: dev (hot reload via `ng serve`).
# Same node:26-alpine + bun binary pattern as the build stage above - the
# Angular CLI still needs real Node, bun is just the package manager/runner.
# NOT the default build target: it sits before the final runtime stage below,
# so an untargeted `docker build` still produces the prod nginx image.
FROM node:26-alpine AS dev
COPY --from=oven/bun:1.3.14-alpine /usr/local/bin/bun /usr/local/bin/bun

WORKDIR /app

COPY package.json bun.lock ./
RUN bun install --frozen-lockfile

COPY . .

EXPOSE 4200

CMD ["bun", "run", "ng", "serve", "--host", "0.0.0.0", "--proxy-config", "proxy.conf.docker.json"]

# Stage 2: serve the static bundle with nginx.
# nginx:1.31.3-alpine is the version the floating nginx:alpine tag resolved
# to at scaffold time (mainline branch, verified via Docker Hub).
FROM nginx:1.31.3-alpine AS runtime

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
