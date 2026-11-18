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
  echo 'Deployment failed. Restoring previous release.' >&2
  deploy "$backup/app.jar" "$backup/dist" && probe "$backup/dist"
  exit 1
fi
