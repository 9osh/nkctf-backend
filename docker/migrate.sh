#!/usr/bin/env bash
# 对「已存在数据卷」的 Postgres 应用增量迁移（init-db 仅在首次建库时自动执行）。
# 用法：./docker/migrate.sh
set -euo pipefail

CONTAINER="${NKCTF_POSTGRES_CONTAINER:-nkctf-postgres}"
DB_USER="${NKCTF_DB_USER:-nkctf}"
DB_NAME="${NKCTF_DB_NAME:-nkctf}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

if ! docker ps --format '{{.Names}}' | grep -qx "$CONTAINER"; then
  echo "错误: Postgres 容器「${CONTAINER}」未运行。请先: docker compose -f docker/docker-compose.yml up -d" >&2
  exit 1
fi

echo ">>> 对 ${CONTAINER}/${DB_NAME} 应用增量迁移 (02-06)..."
for f in "${SCRIPT_DIR}"/init-db/0[2-9]*.sql "${SCRIPT_DIR}"/init-db/[1-9][0-9]*.sql; do
  [[ -f "$f" ]] || continue
  echo "    $(basename "$f")"
  docker exec -i "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 <"$f"
done

echo ">>> 校验 docker_port 列..."
docker exec "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -tAc \
  "SELECT column_name FROM information_schema.columns WHERE table_schema='public' AND table_name='challenge' AND column_name='docker_port';"
echo "完成。"
