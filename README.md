# Spring + Angular SSO Showcase

A small full-stack reference app demonstrating single sign-on between a Spring
Boot API and an Angular SPA, fronted by Keycloak, all wired together with
Docker Compose.

## What this shows

| Area | What's demonstrated |
| --- | --- |
| Spring Boot | Layered REST API under `/api`, OAuth2 resource server (JWT validation), Spring Data JPA against Postgres, `/actuator/health` |
| Angular | Standalone components, signals, route guards driven by realm roles, an SPA built and served as static assets |
| SSO flow | Authorization Code + PKCE (S256) against Keycloak, role-based access (`user` / `admin`) enforced on both sides |
| Docker | Multi-stage builds for both the backend (Maven wrapper -> slim JRE) and frontend (bun build -> nginx) |
| Testcontainers | Backend integration tests spin up real dependencies instead of mocking the database |
| CI/CD | Path-based change detection, formatting/lint quality gates, tests, Docker image builds, GHCR publish, and a documented (opt-in) deploy + rollback path |

## Architecture

```
                          ┌─────────────────────────────────────────┐
                          │              Keycloak :8081             │
                          │             realm: showcase             │
                          └───────▲─────────────────────────▲───────┘
                                  │ login / tokens          │ token validation
                                  │ (browser, PKCE)         │ (JWT, server-side)
                                  │                         │
   ┌──────────┐    HTTP   ┌───────┴───────┐   /api/*  ┌─────┴─────┐   JDBC   ┌────────────┐
   │ Browser  │──────────▶│ nginx :4200   │──────────▶│ backend   │─────────▶│ postgres   │
   │          │◀──────────│ (frontend)    │◀──────────│ :8080     │◀─────────│ :5432      │
   └──────────┘   static  └───────────────┘    JSON   └───────────┘          └────────────┘
                 assets +
                 SPA
```

The browser talks to Keycloak directly for the login redirect (PKCE), and to
nginx for everything else; nginx proxies `/api/*` through to the backend.

## Quick start

```bash
cp .env.example .env   # optional - defaults work out of the box
docker compose up --build
```

| Service | URL |
| --- | --- |
| Frontend (app) | http://localhost:4200 |
| Backend API + Swagger/OpenAPI | http://localhost:8080/api, http://localhost:8080/actuator/health |
| Keycloak | http://localhost:8081 |

The `keycloak-init` service seeds the two demo users the first time the
realm comes up, then exits - `docker compose ps` will show it as
`Exited (0)`, which is expected.

## Demo login

Two realm users are seeded on first boot: `demo` (role `user`) and `admin`
(roles `user` + `admin`). Passwords are **not** printed here on purpose (see
"Keycloak users & secrets" below) - they're whatever `DEMO_USER_PASSWORD` /
`DEMO_ADMIN_PASSWORD` resolve to, which defaults to the values documented in
[`.env.example`](.env.example).

## Local dev (without Docker)

Requires a running Postgres + Keycloak - the quickest way is
`docker compose up postgres keycloak keycloak-init` and then run the two
apps directly on the host:

**Backend**
```bash
cd backend
./mvnw spring-boot:run
```

**Frontend**
```bash
cd frontend
bun install
bun start   # ng serve, proxies /api to the backend per proxy.conf.json
```

## Tests

```bash
cd backend && ./mvnw spotless:check   # formatting check (run spotless:apply to fix)
cd backend && ./mvnw verify           # unit + Testcontainers integration tests
cd frontend && bun run lint          # angular-eslint
cd frontend && bun run test          # headless unit tests
```

## CI/CD

### `.github/workflows/ci.yml` - runs on every push to `main` and on pull requests

1. **changes** - `dorny/paths-filter` compares the diff against the PR base
   (or the previous commit on push) and exposes `backend` / `frontend` /
   `docker` / `workflows` booleans. Every other job is gated on these, so a
   frontend-only PR doesn't pay for a Testcontainers run. Any change under
   `.github/workflows/**` forces every job to run (so you can trust a
   pipeline-editing PR is fully validated before merge). **Pushes to
   `main` always run every job**, regardless of what changed, so `main`
   never merges on a partially-validated pipeline.
2. **backend** *(gated on backend/workflow changes, or push)* - quality
   gate first (`./mvnw spotless:check`, Google Java Format via Spotless),
   then `./mvnw verify` on Temurin JDK 21 (unit + Testcontainers
   integration tests; the Docker daemon is already available on the
   runner).
3. **frontend** *(gated on frontend/workflow changes, or push)* -
   `bun install --frozen-lockfile`, quality gate (`bun run lint` /
   angular-eslint), headless unit tests, production build.
4. **docker** *(gated on backend/frontend/docker/workflow changes, or
   push)* - builds both multi-stage `Dockerfile`s with Buildx to catch
   build breakage early; on push to `main` it additionally logs in to GHCR
   and pushes `latest` + commit-sha tags for both images.
5. **deploy** *(present but fully commented out)* - a ready-to-uncomment
   template (SSH `docker compose pull && up -d`, with a Kubernetes/Helm
   alternative sketched in comments) for wiring up continuous deployment
   once there's somewhere to deploy to. See the `# UNCOMMENT AND CONFIGURE`
   block at the bottom of `ci.yml` for the secrets it needs
   (`DEPLOY_HOST`, `DEPLOY_USER`, `DEPLOY_SSH_KEY`, optional `DEPLOY_PORT`).

### `.github/workflows/rollback.yml` - manual rollback

A `workflow_dispatch` you trigger by hand from the Actions tab, taking
which image(s) to roll back (`backend` / `frontend` / `both`) and the GHCR
tag or commit sha to roll back to. It re-points the `:latest` tag at that
existing image (via `docker buildx imagetools create`) without rebuilding
anything, so a bad `main` push can be un-done immediately. It also carries
a matching fully-commented `redeploy` job template for when the deploy
job above is enabled.

## Keycloak issuer / hostname

This is the one non-obvious piece of wiring in the compose file, so it's
documented here rather than only in a code comment.

The browser and the backend reach Keycloak through **two different
addresses**: the browser hits the published port, `http://localhost:8081`;
the backend, running in its own container, would normally reach Keycloak
over the compose network as `http://keycloak:8080`. A JWT's `iss` claim has
to be one fixed string, and Spring's resource-server validator requires the
`issuer-uri` it's configured with to match that string exactly - so those
two addresses can't just be used interchangeably.

The fix used here has two parts:

- **Keycloak** is started with `KC_HOSTNAME=http://localhost:8081`. This
  pins the issuer Keycloak reports in every token and in its discovery
  document to that one value, regardless of which network a request
  physically came in on (confirmed against current Keycloak hostname-v2
  docs: the issuer is always derived from the configured *frontend* URL,
  never from the request that asked for it).
- **The backend** validates `iss` against that same value
  (`KEYCLOAK_ISSUER_URI=http://localhost:8081/realms/showcase`, matching the
  contract default) but is *not* told to fetch Keycloak's discovery document
  from it - `localhost:8081` isn't reachable from inside the backend's own
  container. Instead, `docker-compose.yml` sets
  `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI` directly (a
  standard Spring Boot property, applied via env-var relaxed binding on top
  of whatever the backend's own `application.yml` already does) to
  `http://keycloak:8080/realms/showcase/protocol/openid-connect/certs` - the
  internal, compose-network address. Spring Security explicitly supports
  configuring `issuer-uri` and `jwk-set-uri` independently for exactly this
  case: `issuer-uri` is used purely to check the `iss` claim (a string
  compare, no network call), while `jwk-set-uri` is used to actually fetch
  the signing keys.

Net effect: no discovery call ever crosses the browser/container network
boundary, the frontend is unaffected (it talks to Keycloak the same way
either way), and the backend can both validate and verify tokens correctly.

## Keycloak users & secrets

`infra/keycloak/realm-showcase.json` (the realm import) intentionally
contains **no users** - only the realm, the `showcase-frontend` public
client (Authorization Code + PKCE), and the `user`/`admin` realm roles. The
`keycloak-init` compose service creates the demo users after Keycloak is
healthy, reading passwords from environment variables
(`DEMO_USER_PASSWORD`, `DEMO_ADMIN_PASSWORD`, and the Keycloak admin
credentials) so that no username/password pair ever appears as a literal in
a committed file. `.env.example` is the one place defaults are documented,
which is the intended, documented location for them.

## Environment variables

| Variable | Default | Used by |
| --- | --- | --- |
| `POSTGRES_DB` | `showcase` | postgres |
| `POSTGRES_USER` | `showcase` | postgres |
| `POSTGRES_PASSWORD` | `showcase` | postgres |
| `DB_HOST` | `postgres` | backend |
| `DB_PORT` | `5432` | backend |
| `DB_NAME` | `showcase` | backend |
| `DB_USER` | `showcase` | backend |
| `DB_PASSWORD` | `showcase` | backend |
| `SPRING_PROFILES_ACTIVE` | `docker` | backend |
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8081/realms/showcase` | backend |
| `KEYCLOAK_ADMIN` | `admin` | keycloak, keycloak-init |
| `KEYCLOAK_ADMIN_PASSWORD` | `admin` | keycloak, keycloak-init |
| `DEMO_USER_PASSWORD` | `demo1234` | keycloak-init |
| `DEMO_ADMIN_PASSWORD` | `admin1234` | keycloak-init |

See [`.env.example`](.env.example) for a ready-to-copy file with all of the
above.
