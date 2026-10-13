#!/usr/bin/env bash
# Belirli bir imaj sürümünü (commit SHA etiketi) kesintisiz yayına alır ve kaydeder.
# Hem normal deploy hem rollback bu scripti kullanır.
#
# Kullanım (compose dosyasının bulunduğu dizinde):  release.sh <servis> <sha|previous>
#   previous: .deploy-history içinde, şu an çalışandan farklı en son sürüme döner.
#
# Dosyalar (compose dizininde): .current-tag (çalışan sürüm), .deploy-history ("<UTC zaman> <sha>").
# Başarılı deploy sonrası, son KEEP_VERSIONS (varsayılan 5) sürümün dışındaki eski SHA etiketli
# imajlar sunucudan silinir (rollback için son sürümler kalır). Temizlik hatası deploy'u bozmaz.
# Not: Bu scripti kullanmadan elle `docker compose up -d` çalıştırmayın; IMAGE_TAG verilmezse
# compose :latest'e düşer ve çalışan sürümle kayıt tutarsız olur.
set -euo pipefail

SERVICE="${1:?Kullanım: release.sh <servis> <sha|previous>}"
TAG="${2:?Kullanım: release.sh <servis> <sha|previous>}"
HERE="$(cd "$(dirname "$0")" && pwd)"
cd "$HERE"

if [ "$TAG" = "previous" ]; then
  CURRENT="$(cat .current-tag 2>/dev/null || true)"
  TAG="$(awk -v c="$CURRENT" '$2 != c { p = $2 } END { print p }' .deploy-history 2>/dev/null || true)"
  [ -n "$TAG" ] || { echo "Geri dönülecek önceki sürüm bulunamadı (.deploy-history boş)" >&2; exit 1; }
  echo "[$SERVICE] önceki sürüm: $TAG"
fi

export IMAGE_TAG="$TAG"
echo "[$SERVICE] sürüm: $TAG"
# SKIP_PULL yalnızca yerel testler içindir (imaj registry'de değil, yerelde).
[ "${SKIP_PULL:-}" = "1" ] || docker compose pull "$SERVICE"
"$HERE/rollout.sh" "$SERVICE" "${ROLLOUT_TIMEOUT:-180}"

echo "$TAG" > .current-tag
echo "$(date -u +%Y-%m-%dT%H:%M:%SZ) $TAG" >> .deploy-history
echo "[$SERVICE] yayında: $TAG"

# --- Eski imaj temizliği (best-effort) ---
prune_old_images() {
  local keep="${KEEP_VERSIONS:-5}" cid image repo tag
  cid="$(docker compose ps -q "$SERVICE" | head -n 1)"
  [ -n "$cid" ] || return 0
  image="$(docker inspect -f '{{.Config.Image}}' "$cid")"
  repo="${image%:*}"

  # Tutulacaklar: kayıttaki son N farklı sürüm (en yeniden eskiye) + çalışan sürüm + latest.
  local keep_tags
  keep_tags="$(awk '{ a[NR] = $2 } END { for (i = NR; i >= 1; i--) print a[i] }' .deploy-history \
    | awk '!seen[$0]++' | head -n "$keep")"
  keep_tags="$keep_tags"$'\n'"$TAG"$'\n'"latest"

  docker images --format '{{.Tag}}' "$repo" | while read -r tag; do
    [ "$tag" = "<none>" ] && continue
    if ! printf '%s\n' "$keep_tags" | grep -qxF "$tag"; then
      echo "[$SERVICE] eski imaj siliniyor: $repo:$tag"
      docker image rm "$repo:$tag" >/dev/null 2>&1 || true
    fi
  done
}
prune_old_images || echo "[$SERVICE] imaj temizliği atlandı (deploy etkilenmedi)" >&2
