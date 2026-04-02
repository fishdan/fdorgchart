#!/usr/bin/env bash

set -euo pipefail

PORT="${E2E_PORT:-18080}"

required_vars=(
  SPRING_DATASOURCE_URL
  SPRING_DATASOURCE_USERNAME
  SPRING_DATASOURCE_PASSWORD
)

missing_vars=()
for var_name in "${required_vars[@]}"; do
  if [[ -z "${!var_name:-}" ]]; then
    missing_vars+=("$var_name")
  fi
done

if (( ${#missing_vars[@]} > 0 )); then
  printf 'Missing required E2E environment variables: %s\n' "${missing_vars[*]}" >&2
  exit 1
fi

exec bash ./mvnw -q spring-boot:run -Dspring-boot.run.arguments="--server.port=${PORT}"
