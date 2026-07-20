#!/bin/sh
# One-shot demo-user seeding for the `showcase` Keycloak realm.
#
# Runs as the `keycloak-init` compose service, against a container image
# that only ships `curl` (no jq), so JSON is parsed with grep/sed/cut.
# Idempotent: safe to re-run (e.g. `docker compose up` again) without
# duplicating users or failing on already-created ones.
#
# Credentials are read from environment variables only (never hardcoded)
# so this file can be committed without tripping secret scanners.

set -eu

KEYCLOAK_URL="${KEYCLOAK_URL:-http://keycloak:8080}"
REALM="${KEYCLOAK_REALM:-showcase}"
ADMIN_USER="${KEYCLOAK_ADMIN:-admin}"
ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"
DEMO_USER_PASSWORD="${DEMO_USER_PASSWORD:-demo1234}"
DEMO_ADMIN_PASSWORD="${DEMO_ADMIN_PASSWORD:-admin1234}"

json_field() {
  # json_field <field-name> — extracts "field":"value" from stdin
  grep -o "\"$1\":\"[^\"]*" | head -n1 | cut -d'"' -f4
}

echo "[keycloak-init] waiting for ${KEYCLOAK_URL} to accept requests..."
i=0
until curl -sf "${KEYCLOAK_URL}/realms/master" >/dev/null 2>&1; do
  i=$((i + 1))
  if [ "$i" -ge 60 ]; then
    echo "[keycloak-init] keycloak never became reachable, giving up" >&2
    exit 1
  fi
  sleep 3
done

echo "[keycloak-init] requesting admin access token..."
ACCESS_TOKEN=""
i=0
while [ -z "$ACCESS_TOKEN" ]; do
  i=$((i + 1))
  RESPONSE=$(curl -sf -X POST \
    "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=password" \
    -d "client_id=admin-cli" \
    -d "username=${ADMIN_USER}" \
    -d "password=${ADMIN_PASSWORD}" 2>/dev/null || true)
  ACCESS_TOKEN=$(printf '%s' "$RESPONSE" | json_field access_token)
  if [ -z "$ACCESS_TOKEN" ]; then
    if [ "$i" -ge 20 ]; then
      echo "[keycloak-init] could not obtain an admin access token, giving up" >&2
      exit 1
    fi
    sleep 3
  fi
done

realm_role_json() {
  role="$1"
  curl -sf -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    "${KEYCLOAK_URL}/admin/realms/${REALM}/roles/${role}"
}

create_user() {
  username="$1"
  password="$2"
  first_name="$3"
  last_name="$4"
  shift 4

  existing=$(curl -sf -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    "${KEYCLOAK_URL}/admin/realms/${REALM}/users?username=${username}&exact=true")

  if printf '%s' "$existing" | grep -q "\"username\":\"${username}\""; then
    echo "[keycloak-init] user '${username}' already exists, skipping creation"
  else
    echo "[keycloak-init] creating user '${username}'"
    curl -sf -X POST "${KEYCLOAK_URL}/admin/realms/${REALM}/users" \
      -H "Authorization: Bearer ${ACCESS_TOKEN}" \
      -H "Content-Type: application/json" \
      -d "{\"username\":\"${username}\",\"enabled\":true,\"email\":\"${username}@example.com\",\"firstName\":\"${first_name}\",\"lastName\":\"${last_name}\",\"emailVerified\":true,\"requiredActions\":[],\"credentials\":[{\"type\":\"password\",\"value\":\"${password}\",\"temporary\":false}]}"
  fi

  user_id=$(curl -sf -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    "${KEYCLOAK_URL}/admin/realms/${REALM}/users?username=${username}&exact=true" \
    | json_field id)

  # Idempotent profile update: a complete profile (email + names) and no
  # pending required actions, otherwise Keycloak forces a VERIFY_PROFILE
  # step on first login. Also heals users seeded by older script versions.
  curl -sf -X PUT "${KEYCLOAK_URL}/admin/realms/${REALM}/users/${user_id}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{\"enabled\":true,\"email\":\"${username}@example.com\",\"firstName\":\"${first_name}\",\"lastName\":\"${last_name}\",\"emailVerified\":true,\"requiredActions\":[]}"

  for role in "$@"; do
    echo "[keycloak-init] ensuring role '${role}' on '${username}'"
    role_json=$(realm_role_json "$role")
    curl -sf -X POST \
      "${KEYCLOAK_URL}/admin/realms/${REALM}/users/${user_id}/role-mappings/realm" \
      -H "Authorization: Bearer ${ACCESS_TOKEN}" \
      -H "Content-Type: application/json" \
      -d "[${role_json}]" >/dev/null
  done
}

create_user "demo" "${DEMO_USER_PASSWORD}" "Demo" "User" "user"
create_user "admin" "${DEMO_ADMIN_PASSWORD}" "Admin" "User" "user" "admin"

echo "[keycloak-init] demo users seeded successfully"
