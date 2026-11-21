#!/usr/bin/env bash
set -euo pipefail
umask 077

release=$(realpath "${1:?release path required}")
case "$release" in
  /opt/itsworld/releases/*) ;;
  *) exit 2 ;;
esac

cd /opt/itsworld
exec 9>.release.lock
flock -n 9
(cd "$release" && sha256sum -c SHA256SUMS)
test -f app.jar
test -f dist/index.html
docker compose config --quiet

mkdir "$release/staged"
tar -xzf "$release/frontend.tar.gz" -C "$release/staged"
test -f "$release/staged/dist/index.html"
chmod 755 "$release/staged/dist"
mkdir -p backups
backup="backups/$(basename "$release")"
mkdir "$backup"
cp -p app.jar "$backup/app.jar"
cp -a dist "$backup/dist"

# 迁移失败时保留旧应用运行。pipefail 确保备份失败时中断发布。
docker compose exec -T mysql sh -c \
  'export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"; exec mysqldump -uroot --single-transaction --routines --triggers --set-gtid-purged=OFF --no-tablespaces "$MYSQL_DATABASE"' \
  | gzip > "$backup/database.sql.gz"
gzip -t "$backup/database.sql.gz"
docker compose run --rm --no-deps -v "$release/app.jar:/migration/app.jar:ro" server \
  java -Xmx128m -Dloader.main=com.itsworld.migration.MigrationRunner \
  -cp /migration/app.jar org.springframework.boot.loader.launch.PropertiesLauncher migrate

deploy() {
  local jar=$1 assets=$2
  # Preserve bind mount paths and inodes while replacing their contents.
  docker compose stop web server || return
  cp "$jar" app.jar || return
  cp -a "$assets/." dist/ || return
  docker compose start server web
}

probe() {
  local code
  for i in {1..30}; do
    code=$(curl -sS --max-time 4 --resolve lvhaorankk.top:443:127.0.0.1 \
      -o /dev/null -w '%{http_code}' https://lvhaorankk.top/api/assistant/sessions) || true
    if [[ $code == 401 ]] && curl -fsS --max-time 4 --resolve lvhaorankk.top:443:127.0.0.1 \
        https://lvhaorankk.top/ | cmp -s - "$1/index.html"; then
      return 0
    fi
    sleep 2
  done
  return 1
}

if deploy "$release/app.jar" "$release/staged/dist" && probe "$release/staged/dist"; then
  printf 'Deployment verified. Backup: %s\n' "$backup"
else
  echo 'Deployment failed. Restoring application files; database migrations remain applied.' >&2
  deploy "$backup/app.jar" "$backup/dist" && probe "$backup/dist"
  exit 1
fi
