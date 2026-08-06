#!/usr/bin/env bash

set -euo pipefail

image_name="${1:-ms079-server:ci}"
repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
run_suffix="${GITHUB_RUN_ID:-local}-$$"
mysql_container="ms079-mysql-ci-${run_suffix}"
server_container="ms079-server-ci-${run_suffix}"
mysql_password="ms079-ci-root-password"
mysql_image="mysql:5.7@sha256:4bc6bc963e6d8443453676cae56536f4b8156d78bae03c0145cbe47c2aad73bb"
ready_message="服务端启动完毕"

cleanup() {
  docker rm -f "${server_container}" >/dev/null 2>&1 || true
  docker rm -f "${mysql_container}" >/dev/null 2>&1 || true
}
trap cleanup EXIT

docker run --detach \
  --platform linux/amd64 \
  --name "${mysql_container}" \
  --env "MYSQL_ROOT_PASSWORD=${mysql_password}" \
  --env MYSQL_DATABASE=ms079 \
  --health-cmd='mysqladmin ping --host=127.0.0.1 --user=root --password="$MYSQL_ROOT_PASSWORD" --silent' \
  --health-interval=2s \
  --health-timeout=3s \
  --health-retries=60 \
  "${mysql_image}" >/dev/null

for _ in $(seq 1 60); do
  mysql_health="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "${mysql_container}")"
  if [[ "${mysql_health}" == "healthy" ]]; then
    break
  fi
  if [[ "${mysql_health}" == "unhealthy" ]]; then
    docker logs "${mysql_container}"
    echo "MySQL 5.7 container became unhealthy" >&2
    exit 1
  fi
  sleep 2
done

if [[ "$(docker inspect --format '{{.State.Health.Status}}' "${mysql_container}")" != "healthy" ]]; then
  docker logs "${mysql_container}"
  echo "Timed out waiting for MySQL 5.7" >&2
  exit 1
fi

docker exec --interactive --env "MYSQL_PWD=${mysql_password}" "${mysql_container}" \
  mysql --binary-mode=1 --user=root ms079 \
  < "${repository_root}/db/ms079.sql"

table_count="$(docker exec --env "MYSQL_PWD=${mysql_password}" "${mysql_container}" \
  mysql --batch --skip-column-names --user=root \
  --execute="SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'ms079';")"
if [[ ! "${table_count}" =~ ^[0-9]+$ ]] || (( table_count < 50 )); then
  echo "Imported schema contains an unexpected number of tables: ${table_count}" >&2
  exit 1
fi

docker exec --env "MYSQL_PWD=${mysql_password}" "${mysql_container}" \
  mysql --batch --skip-column-names --user=root ms079 \
  --execute="SELECT id, name FROM accounts LIMIT 1; SELECT id FROM characters LIMIT 1;" \
  >/dev/null

docker run --detach \
  --name "${server_container}" \
  --network "container:${mysql_container}" \
  --volume "${repository_root}/wz:/app/wz:ro" \
  --volume "${repository_root}/脚本:/app/脚本:ro" \
  --volume "${repository_root}/.github/ci/server-ci.ini:/app/服务端配置.ini:ro" \
  "${image_name}" >/dev/null

server_ready=false
for _ in $(seq 1 120); do
  if [[ "$(docker inspect --format '{{.State.Running}}' "${server_container}")" != "true" ]]; then
    docker logs "${server_container}"
    echo "Server container exited before becoming ready" >&2
    exit 1
  fi
  if docker logs "${server_container}" 2>&1 | grep --fixed-strings --quiet "${ready_message}"; then
    server_ready=true
    break
  fi
  sleep 2
done

if [[ "${server_ready}" != "true" ]]; then
  docker logs "${server_container}"
  echo "Timed out waiting for the server readiness message" >&2
  exit 1
fi

docker exec "${mysql_container}" bash -eu -c '
  for port in 9595 7576 7577 7578 8600; do
    timeout 2 bash -c "</dev/tcp/127.0.0.1/${port}" || {
      echo "Expected server port is not listening: ${port}" >&2
      exit 1
    }
  done
'

echo "MySQL schema import and server startup smoke test passed (${table_count} tables)."
