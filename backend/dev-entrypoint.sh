#!/bin/sh
set -e

# First-boot compile: the dev image ships a baseline copy of src/ (see
# Dockerfile), so make sure target/classes is up to date before we start the
# app - otherwise spring-boot:run's first launch would run stale/no classes.
echo "[dev-entrypoint] Initial compile..."
./mvnw -o compile

# Background watcher: recompiles whenever files under src/main change (compose
# watch syncs host edits into this path at runtime). Spring Boot DevTools, on
# the classpath in this dev image only, notices the updated .class files in
# target/classes and performs a fast in-JVM restart - no container restart.
(
  while inotifywait -qq -r -e modify,create,delete,move src/main; do
    # Debounce: drain further events for up to 2s of quiet so a burst of
    # saves (IDE autosave, compose watch syncing several files at once)
    # collapses into a single recompile.
    while inotifywait -qq -r -t 2 -e modify,create,delete,move src/main; do :; done
    echo "[dev-entrypoint] Change detected, recompiling..."
    ./mvnw -o compile || echo "[dev-entrypoint] Compile failed, waiting for next change..."
  done
) &

exec ./mvnw spring-boot:run
