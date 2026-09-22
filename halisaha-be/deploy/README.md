# Deploy Rehberi — Hostinger VPS (Docker + Traefik) + GitHub Actions CI/CD

Bu doküman, `halisaha-be` ve `halisaha-fe` uygulamalarının Hostinger VPS
üzerinde, `main` branch'ine her push'ta otomatik deploy eden bir CI/CD
pipeline ile nasıl canlıda tutulduğunu açıklar.

> Not: Bu repo bir monorepo'dur (`halisaha-app`), `halisaha-be/` ve
> `halisaha-fe/` alt klasörlerini içerir. Her uygulamanın kendi workflow'u
> vardır (`.github/workflows/be-ci-cd.yml`, `fe-ci-cd.yml`) ve yalnızca
> ilgili klasörde değişiklik olduğunda tetiklenir.

Mimari:
- **VPS (Hostinger, Ubuntu 24.04)**: Tek sunucu, Docker + Docker Compose ile
  tüm servisleri çalıştırır.
- **Traefik**: `/docker/traefik` altında ayrı bir compose projesi olarak
  çalışır (`network_mode: host`, Docker socket üzerinden container'ları
  otomatik keşfeder). HTTP→HTTPS yönlendirme ve Let's Encrypt sertifikaları
  (`certresolver=letsencrypt`) bu servis tarafından yönetilir. Uygulama
  container'ları host'a port yayınlamaz; Traefik label'ları üzerinden
  keşfedilir.
- **PostgreSQL**: `halisaha-be` ile aynı compose projesinde, adlandırılmış
  bir volume ile (`halisaha-postgres-data`) veri kalıcılığı sağlanır.
  Dışarıya port açılmaz, yalnızca `app` servisi docker network üzerinden
  erişir.
- **GHCR (GitHub Container Registry)**: Docker imajlarının push edildiği
  yer.
- **GitHub Actions**: `main`'e her push'ta build → test → image push →
  VPS'e SSH ile deploy.
- Domainler: `https://app.halisahapro.tech` (frontend),
  `https://api.halisahapro.tech` (backend).

---

## 1) DNS ayarları

Domain sağlayıcınızda aşağıdaki A kayıtlarını VPS IP'sine yönlendirin:

| Tip | Host | Değer            |
|-----|------|-------------------|
| A   | app  | `<VPS_IP>`        |
| A   | api  | `<VPS_IP>`        |

DNS yayılımı birkaç dakika ile birkaç saat sürebilir. Traefik, ilk istek
geldiğinde Let's Encrypt HTTP-01 challenge ile otomatik sertifika alır; DNS
doğru yönlendirilmeden sertifika alınamaz.

---

## 2) VPS üzerinde deploy kullanıcısı ve dizinler

Bu adımlar bu proje için bir kere yapıldı (referans amaçlı):

```bash
# root olarak:
useradd -m -s /bin/bash -G docker deploy
mkdir -p /home/deploy/.ssh
echo "<github-actions-deploy-public-key>" >> /home/deploy/.ssh/authorized_keys
chmod 700 /home/deploy/.ssh && chmod 600 /home/deploy/.ssh/authorized_keys
chown -R deploy:deploy /home/deploy/.ssh

mkdir -p /opt/halisaha-be /opt/halisaha-fe
chown -R deploy:deploy /opt/halisaha-be /opt/halisaha-fe
```

`deploy` kullanıcısı `docker` grubunda olduğu için `sudo` olmadan Docker
komutlarını çalıştırabilir. GitHub Actions bu kullanıcı ve ona ait SSH
private key ile bağlanır.

---

## 3) Sunucuda `.env` dosyasını oluşturma

`deploy/.env.example` dosyasını referans alarak sunucuda
`/opt/halisaha-be/.env` dosyasını oluşturun:

```bash
ssh -i <deploy-key> deploy@<VPS_IP>
nano /opt/halisaha-be/.env
chmod 600 /opt/halisaha-be/.env
```

Bu dosya **asla git'e commit edilmemeli**; sadece sunucuda durur ve CI/CD
tarafından dokunulmaz (yalnızca `docker-compose.yml` her deploy'da
üzerine yazılır).

Frontend için ayrı bir `.env` gerekmiyor; `VITE_API_BASE_URL` build-time'da
GitHub Actions secret'ı olarak image'a gömülüyor.

---

## 4) GHCR (GitHub Container Registry) imajları

CI/CD ilk kez `main`'e push edildiğinde imajları otomatik olarak
`ghcr.io/<owner>/halisaha-be` ve `ghcr.io/<owner>/halisaha-fe` altına push
eder. Deploy adımı, `docker compose pull` çalıştırmadan önce VPS üzerinde
`GITHUB_TOKEN` ile otomatik `docker login ghcr.io` yaptığı için paketlerin
public olması **gerekmez**; private kalabilirler.

---

## 5) GitHub repository secrets

Repo → **Settings** → **Secrets and variables** → **Actions**:

| Secret adı            | Değer                                                          |
|------------------------|-----------------------------------------------------------------|
| `VPS_HOST`             | VPS IP adresi                                                   |
| `VPS_SSH_USER`         | `deploy`                                                        |
| `VPS_SSH_KEY`          | Deploy amaçlı üretilen SSH private key'in tüm içeriği           |
| `VITE_API_BASE_URL`    | `https://api.halisahapro.tech`                                  |

`GITHUB_TOKEN` otomatik sağlanır, ekstra bir şey yapmanıza gerek yok.

`Settings → Environments` altında `production` adında bir environment
tanımlıdır (workflow'daki `environment: production` alanıyla eşleşir);
isterseniz buraya manuel onay (required reviewers) da ekleyebilirsiniz.

---

## 6) İlk deploy

Her şey hazır olduğunda:

```bash
git push origin main
```

veya GitHub Actions sekmesinden ilgili workflow'u **Run workflow**
(`workflow_dispatch`) ile `main` branch'i seçerek manuel tetikleyin.

Pipeline: `test`/`build` → `build-and-push` → `deploy`. Tamamlandığında
`https://app.halisahapro.tech` ve `https://api.halisahapro.tech` üzerinden
erişebilirsiniz.

---

## Sorun giderme

- **Deploy adımı SSH ile bağlanamıyor**: `VPS_SSH_KEY` secret'ının private
  key'in tam içeriği olduğundan (başında/sonunda fazladan boşluk/satır
  olmadan) ve `VPS_SSH_USER` değerinin `deploy` olduğundan emin olun.
- **`docker compose pull` imajı bulamıyor**: Paketin public olduğundan ve
  imaj adının `ghcr.io/<owner>/halisaha-be`/`halisaha-fe` ile birebir
  eştiğinden emin olun (küçük harf olmalı).
- **HTTPS sertifikası alınamıyor / 404 Bad Gateway**: DNS A kayıtlarının VPS
  IP'sine doğru yönlendirildiğinden ve Traefik label'larındaki `Host(...)`
  kuralının domain ile birebir eştiğinden emin olun. Traefik loglarını
  kontrol edin: `docker logs traefik-traefik-1`.
- **Uygulama postgres'e bağlanamıyor**: `.env` içindeki `DB_USERNAME`/
  `DB_PASSWORD` ile `POSTGRES_USER`/`POSTGRES_PASSWORD` değerlerinin birebir
  aynı olduğundan emin olun.
