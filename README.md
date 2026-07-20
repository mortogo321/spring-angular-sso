# Spring + Angular SSO Showcase

[![CI](https://github.com/mortogo321/spring-angular-sso/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/mortogo321/spring-angular-sso/actions/workflows/ci.yml?query=branch%3Amain)

A small full-stack reference app demonstrating single sign-on between a Spring
Boot API and an Angular SPA, fronted by Keycloak, all wired together with
Docker Compose.

## What this shows

| Area | What's demonstrated |
| --- | --- |
| Spring Boot | Layered REST API under `/api`, OAuth2 resource server (JWT validation + realm-role mapping), Spring Data JPA against Postgres, Flyway migrations, Bean Validation, RFC 9457 `ProblemDetail` error responses, springdoc OpenAPI/Swagger UI with OAuth2 login, `/actuator/health` |
| Angular | Standalone components only (no NgModules), zoneless signals-based state, `@if`/`@for` control flow, lazy `loadComponent` routes, functional guards + bearer-token interceptor, typed reactive forms, `keycloak-angular` PKCE integration, Vitest unit tests |
| SSO flow | Authorization Code + PKCE (S256) against Keycloak, role-based access (`user` / `admin`) enforced on both sides |
| Docker | Multi-stage builds for both the backend (Maven wrapper -> slim JRE) and frontend (bun build -> nginx) |
| Testcontainers | Backend integration tests spin up real dependencies instead of mocking the database |
| CI/CD | Staged pipeline (Code Quality -> Test -> Build) with parallel backend/frontend lanes, path-based step skipping (Markdown-only commits trigger no run), GHCR publish, and a documented (opt-in) deploy + rollback path |

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

## Project layout

Each stack is self-contained - its own `Dockerfile`, its own `.gitignore`,
its own toolchain - so either can be extracted and used standalone. The
root only holds orchestration and global config.

```
├── backend/                  Spring Boot API (Maven)
│   ├── src/                  main + test (unit, security slice, Testcontainers)
│   ├── Dockerfile            multi-stage: Maven build -> slim JRE runtime
│   └── .gitignore            Maven/JVM-specific ignores
├── frontend/                 Angular SPA (bun)
│   ├── src/                  standalone components, signals, keycloak-angular
│   ├── Dockerfile            multi-stage: bun build -> nginx runtime
│   ├── nginx.conf            SPA fallback + /api reverse proxy
│   └── .gitignore            Node/Angular-specific ignores
├── infra/keycloak/           realm import (no users) + demo-user seed script
├── .github/workflows/        ci.yml + rollback.yml
├── docker-compose.yml        postgres + keycloak + keycloak-init + backend + frontend
├── .env.example              every variable with its default
└── .gitignore                global concerns only (env, OS, editors, logs)
```

## Quick start

```bash
cp .env.example .env   # optional - defaults work out of the box
docker compose up --build
```

| Service | URL |
| --- | --- |
| Frontend (app) | http://localhost:4200 |
| Backend API | http://localhost:8080/api/tasks |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| Backend health | http://localhost:8080/actuator/health |
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

## Dev mode (hot reload)

Run the whole stack in containers with live reload on both sides - no local
JDK or Angular CLI toolchain needed:

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml watch
```

This builds `backend`/`frontend` from their Dockerfile `dev` stages (instead
of the prod `runtime`/nginx images) and uses [Compose
Watch](https://docs.docker.com/compose/how-tos/file-watch/) to sync source
changes into the running containers:

- **Frontend** - `ng serve` runs inside the container; Compose Watch syncs
  `frontend/src` and `frontend/public` into it, and Angular's own dev-server
  live reload (HMR) picks up the change in the browser almost immediately
  (well under a second, no page refresh needed for most template/style
  edits). Editing `package.json` or `bun.lock` triggers a full image rebuild
  (fresh `bun install`) instead.
- **Backend** - Compose Watch syncs `backend/src` into the container, where
  an `inotifywait` loop recompiles changed classes with `./mvnw -o compile`.
  Spring Boot DevTools (present on the classpath only in the `dev` image)
  detects the updated `.class` files and performs an in-JVM restart - a few
  seconds, not a container rebuild. Editing `pom.xml` triggers a full image
  rebuild instead.

The prod flow (`docker compose up --build` against plain `docker-compose.yml`)
is unchanged - the `dev` build stages are opt-in and are never selected unless
targeted explicitly (via this overlay file).

## Tests

```bash
cd backend && ./mvnw spotless:check   # formatting check (run spotless:apply to fix)
cd backend && ./mvnw verify           # unit + Testcontainers integration tests
cd frontend && bun run lint          # angular-eslint
cd frontend && bun run test          # headless unit tests
```

## CI/CD

### `.github/workflows/ci.yml` - runs on pushes to `main` and on pull requests (Markdown-only changes trigger no run at all)

A staged pipeline - **Code Quality &rarr; Test &rarr; Build (&rarr; Deploy,
commented out)** - split into a backend lane and a frontend lane. The two
lanes run in parallel (each stack's Test needs only its own Code Quality)
and converge at Build via `needs:`. Every job runs its own
`dorny/paths-filter` step (no shared upstream "changes" job) to compute
just the booleans it gates its steps on - so a frontend-only PR skips the
backend steps of every stage instead of paying for a Testcontainers run.
Markdown-only changes don't start the workflow at all (workflow-level
`paths-ignore`). Any change
under `.github/workflows/**` forces every gated step to run (so you can
trust a pipeline-editing PR is fully validated before merge). **Whenever a
push to `main` triggers the workflow, every step runs**, regardless of
which paths changed, so `main` never merges on a partially-validated
pipeline. Because only steps skip
(not whole jobs), the `needs:` chain is a plain dependency graph.

1. **Code Quality** *(jobs `quality-backend`, `quality-frontend`)* - the
   backend lane sets up Temurin JDK 21 and runs `./mvnw spotless:check`
   *(gated on backend/workflow changes, or push)*; the frontend lane sets
   up Bun, installs dependencies, and runs `bun run lint` / angular-eslint
   *(gated on frontend/workflow changes, or push)*.
2. **Test** *(jobs `test-backend`, `test-frontend`, each needing its
   lane's quality job)* - the backend lane runs `./mvnw verify` on Temurin
   JDK 21 (unit + Testcontainers integration tests; the Docker daemon is
   already available on the runner) *(gated on backend/workflow changes,
   or push)*; the frontend lane runs `bun install --frozen-lockfile`,
   headless unit tests, and a production build *(gated on
   frontend/workflow changes, or push)*.
3. **Build** *(job `build`, needs both test jobs)* - builds both multi-stage
   `Dockerfile`s with Buildx to catch build breakage early *(gated on
   docker-relevant changes - backend, frontend, or compose file - workflow
   changes, or push)*; on push to `main`
   it additionally logs in to GHCR and pushes `latest` + commit-sha tags
   for both images.
4. **Deploy** *(job `deploy`, needs `build`; present but fully commented
   out)* - a ready-to-uncomment template (SSH `docker compose pull && up
   -d`, with a Kubernetes/Helm alternative sketched in comments) for
   wiring up continuous deployment once there's somewhere to deploy to.
   See the `# UNCOMMENT AND CONFIGURE` block at the bottom of `ci.yml` for
   the secrets it needs (`DEPLOY_HOST`, `DEPLOY_USER`, `DEPLOY_SSH_KEY`,
   optional `DEPLOY_PORT`).

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
