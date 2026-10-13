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

## Kesintisiz deploy (rolling)

Deploy adımı `docker compose up -d` yerine `rollout.sh <servis>` çalıştırır
(`halisaha-be/deploy/rollout.sh`, sunucuda `/opt/halisaha-*/rollout.sh`):

1. Servis 2 kat ölçeklenir; eski container'a dokunulmaz.
2. Yeni container'ın healthcheck'i `healthy` olunca Traefik ona trafik yollamaya başlar
   (Traefik sağlıksız/başlamakta olan container'ı rotaya almaz).
3. Eski container durdurulup silinir.
4. Yeni container zaman aşımı süresi (varsayılan 180 sn, ikinci argüman) içinde healthy olmazsa yeni container silinir,
   eski çalışmaya devam eder ve deploy adımı hata verir.

Gereksinimler: servis tanımında `healthcheck` olmalı, `container_name` kullanılmamalı.
BE healthcheck'i Spring Actuator'ın ayrı yönetim portunda (`8081`, `MANAGEMENT_PORT`) çalışır;
bu port Traefik'e açılmaz, dışarıdan erişilemez.

Dikkat: deploy sırasında eski ve yeni sürüm aynı veritabanını birlikte kullanır. Flyway
migration'ları eski sürümle uyumlu olmalı (sütun silme/yeniden adlandırma gibi kırıcı değişiklikleri
iki aşamada yapın).

---

## Sürümlü deploy ve geri alma (rollback)

Her deploy, imajı `:latest` yerine commit SHA etiketiyle (`IMAGE_TAG`) yayına alır
(`deploy/release.sh <servis> <sha|previous>`). Sunucuda her uygulama dizininde:

- `.current-tag`: şu an çalışan sürüm (SHA)
- `.deploy-history`: başarılı deploy'ların kaydı (`<UTC zaman> <sha>`)

**Geri alma:** GitHub → Actions → **Rollback** → *Run workflow*. `app` (be/fe) seçin,
`version` alanına bir SHA ya da `previous` yazın (`previous`: şu an çalışandan farklı en son sürüm).
Build gerekmez, imaj GHCR'da hazırdır ve geçiş yine kesintisizdir (birkaç saniye).
Hangi SHA'lar var: sunucuda `tail /opt/halisaha-<be|fe>/.deploy-history`.

Notlar:
- Rollback yalnızca çalışan sürümü değiştirir. `main`'deki hatalı commit yerinde durur; bir sonraki
  push yeniden o kodu deploy eder. Kalıcı çözüm için hatalı değişikliği `main`'de revert edin.
- **Veritabanı migration'ları geri alınmaz.** Eski kod yeni şemayla çalışabiliyorsa rollback yeterlidir;
  sütun silen/veriyi dönüştüren bir migration yayına çıktıysa yedekten dönmek gerekir (bkz. `YEDEK.md`).
- Sunucuda `release.sh` dışında elle `docker compose up -d` çalıştırmayın: `IMAGE_TAG` verilmezse
  servis `:latest`'e düşer ve `.current-tag` ile çalışan sürüm tutarsız olur.
- İlk deploy'dan sonra geçmişte tek kayıt olur; `previous` ikinci deploy'dan itibaren kullanılabilir.
- Eski imaj temizliği otomatiktir: her başarılı deploy sonrası `release.sh`, kayıttaki son 5 sürümün
  (ve çalışan sürümün) dışındaki SHA etiketli imajları sunucudan siler. Sayı `KEEP_VERSIONS`
  ortam değişkeniyle değişir. Rollback bu son 5 sürüme yapılabilir; daha eskisi için imaj GHCR'dan
  yeniden çekilir (SHA hâlâ geçerliyse rollback workflow'u bunu kendisi yapar). Temizlik hatası deploy'u bozmaz.

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
