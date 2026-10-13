#!/usr/bin/env bash
# Kesintisiz (rolling) deploy: bir compose servisini, yeni container sağlıklı (healthy)
# olana kadar eskisini çalışır tutarak günceller.
#
# Kullanım (compose dosyasının bulunduğu dizinde):  rollout.sh <servis> [bekleme-saniyesi]
#
# Akış: yeni imaj zaten pull edilmiş olmalı -> servis 2 kat ölçeklenir (eski container'a
# dokunulmaz) -> yeni container healthcheck'ten geçince Traefik ona trafik yollamaya başlar
# -> eski container durdurulup silinir -> ölçek eski haline döner.
# Servis daha önce hiç çalışmıyorsa düz `up -d` yapar.
# Gereksinim: servis tanımında healthcheck olmalı ve `container_name` kullanılmamalı.
set -euo pipefail

SERVICE="${1:?Kullanım: rollout.sh <servis> [bekleme-saniyesi]}"
TIMEOUT="${2:-180}"

OLD=($(docker compose ps -q "$SERVICE" 2>/dev/null || true))
if [ "${#OLD[@]}" -eq 0 ]; then
  echo "[$SERVICE] çalışan container yok, düz up -d yapılıyor"
  docker compose up -d --no-deps "$SERVICE"
  exit 0
fi

COUNT="${#OLD[@]}"
echo "[$SERVICE] ${COUNT} eski container var, $((COUNT * 2))'e ölçekleniyor"
docker compose up -d --no-deps --no-recreate --scale "$SERVICE=$((COUNT * 2))" "$SERVICE"

ALL=($(docker compose ps -q "$SERVICE"))
NEW=()
for id in "${ALL[@]}"; do
  [[ " ${OLD[*]} " == *" $id "* ]] || NEW+=("$id")
done

rollback() {
  echo "[$SERVICE] yeni container sağlıklı olmadı, geri alınıyor (eski container çalışmaya devam ediyor)" >&2
  docker logs --tail 60 "${NEW[0]}" >&2 || true
  docker rm -f "${NEW[@]}" >/dev/null
  exit 1
}

echo "[$SERVICE] yeni container(lar) healthy olana kadar bekleniyor (en fazla ${TIMEOUT}s)"
deadline=$((SECONDS + TIMEOUT))
for id in "${NEW[@]}"; do
  while :; do
    status="$(docker inspect -f '{{if .State.Running}}{{if .State.Health}}{{.State.Health.Status}}{{else}}nohealth{{end}}{{else}}stopped{{end}}' "$id")"
    [ "$status" = "healthy" ] && break
    if [ "$status" = "nohealth" ]; then
      echo "[$SERVICE] servis tanımında healthcheck yok, rollout yapılamaz" >&2
      rollback
    fi
    if [ "$status" = "stopped" ] || [ "$SECONDS" -ge "$deadline" ]; then
      rollback
    fi
    sleep 2
  done
done

# Traefik'in yeni container'ı keşfedip rotaya almasına zaman tanı, sonra eskiyi kaldır.
sleep 5
echo "[$SERVICE] eski container(lar) kaldırılıyor"
docker stop -t 30 "${OLD[@]}" >/dev/null
docker rm "${OLD[@]}" >/dev/null
docker compose up -d --no-deps --no-recreate --scale "$SERVICE=$COUNT" "$SERVICE"
echo "[$SERVICE] rollout tamam"
