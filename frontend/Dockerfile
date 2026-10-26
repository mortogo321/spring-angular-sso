# syntax=docker/dockerfile:1.7

# Stage 1: build the Angular production bundle with bun.
# Node 26 base + the bun binary: bun is the package manager/script runner,
# but the Angular CLI checks process.version and rejects bun's emulated Node,
# so `ng` itself must run on real Node.
# oven/bun:1.4.2-alpine matches the bun version pinned in this repo.
FROM node:26.10-alpine AS build
COPY --from=oven/bun:1.4.2-alpine /usr/local/bin/bun /usr/local/bin/bun

WORKDIR /app

COPY package.json bun.lock ./
RUN bun install --frozen-lockfile

COPY . .
RUN bun run build

# Stage: dev (hot reload via `ng serve`).
# Same node:26.10-alpine + bun binary pattern as the build stage above - the
# Angular CLI still needs real Node, bun is just the package manager/runner.
# NOT the default build target: it sits before the final runtime stage below,
# so an untargeted `docker build` still produces the prod nginx image.
FROM node:26.10-alpine AS dev
COPY --from=oven/bun:1.4.2-alpine /usr/local/bin/bun /usr/local/bin/bun

WORKDIR /app

COPY package.json bun.lock ./
RUN bun install --frozen-lockfile

COPY . .

EXPOSE 4200

CMD ["bun", "run", "ng", "serve", "--host", "0.0.0.0", "--proxy-config", "proxy.conf.docker.json"]

# Stage 2: serve the static bundle with nginx.
FROM nginx:1.31.8-alpine AS runtime

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

# nginx's unprivileged uid is 101; match it rather than running as root.
RUN chown -R 101:101 /usr/share/nginx/html /var/cache/nginx /var/log/nginx \
  && touch /var/run/nginx.pid && chown 101:101 /var/run/nginx.pid
USER 101:101

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -qO- http://127.0.0.1:80/ >/dev/null 2>&1 || exit 1

CMD ["nginx", "-g", "daemon off;"]
