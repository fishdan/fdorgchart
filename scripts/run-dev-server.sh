#!/usr/bin/env bash

set -euo pipefail

PORT="${PORT:-8080}"

required_vars=(
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
  printf 'Missing required local dev environment variables: %s\n' "${missing_vars[*]}" >&2
  printf 'Set SPRING_DATASOURCE_USERNAME and SPRING_DATASOURCE_PASSWORD before starting the dev server.\n' >&2
  exit 1
fi

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"

exec bash ./mvnw -q spring-boot:run -Dspring-boot.run.arguments="--server.port=${PORT}"
