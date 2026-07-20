# syntax=docker/dockerfile:1

# ---- Stage 1: build ----
FROM maven:3.9.16-eclipse-temurin-21-noble AS build
WORKDIR /build

# Copy only the POM first so dependency resolution is cached in its own layer
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

# Now copy sources and build
COPY src/ src/
RUN ./mvnw -B -DskipTests package

# Rename to a fixed name so the runtime stage doesn't need to know the Maven finalName,
# then extract the jar into layers (dependencies / spring-boot-loader / snapshot-dependencies / application)
RUN cp target/backend.jar application.jar \
    && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ---- Stage: dev (hot reload) ----
# Same base image as the build stage above, so JDK/Maven versions never drift
# between prod build and dev. NOT the default build target: it sits before the
# final runtime stage, so an untargeted `docker build` (no --target) still
# stops at, and produces, the prod runtime image below.
FROM maven:3.9.16-eclipse-temurin-21-noble AS dev
WORKDIR /app

# inotify-tools drives the recompile-on-change loop in dev-entrypoint.sh.
RUN apt-get update \
    && apt-get install -y --no-install-recommends inotify-tools \
    && rm -rf /var/lib/apt/lists/*

# Copy only the POM first so dependency resolution is cached in its own layer,
# same as the build stage. This also seeds the /root/.m2 named volume (Docker
# copies image content into a named volume on first mount) used by compose.dev.
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

# Baseline source copy so the container has something to run on first boot;
# compose.dev's `develop.watch` syncs subsequent host changes on top of this.
COPY src/ src/

COPY dev-entrypoint.sh .
RUN chmod +x dev-entrypoint.sh

EXPOSE 8080

ENTRYPOINT ["./dev-entrypoint.sh"]

# ---- Stage 2: runtime ----
FROM eclipse-temurin:21-jre-noble AS runtime
WORKDIR /application

# curl is needed for the HEALTHCHECK below; the base image does not ship it
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system spring && useradd --system --gid spring --no-create-home spring

USER spring

COPY --from=build /build/extracted/dependencies/ ./
COPY --from=build /build/extracted/spring-boot-loader/ ./
COPY --from=build /build/extracted/snapshot-dependencies/ ./
COPY --from=build /build/extracted/application/ ./

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl --fail --silent http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "application.jar"]
