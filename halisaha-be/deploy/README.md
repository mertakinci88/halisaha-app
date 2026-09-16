# Deploy Rehberi — AWS Free Tier (EC2 + RDS) + GitHub Actions CI/CD

Bu doküman, `halisaha-be` uygulamasını AWS Free Tier üzerinde canlıya almak ve
`main` branch'ine her push'ta otomatik deploy eden bir CI/CD pipeline kurmak
için gereken tüm adımları içerir.

> Not: Bu repo bir monorepo'dur (`halisaha-app`), `halisaha-be/` ve
> `halisaha-fe/` alt klasörlerini içerir. Backend workflow'u
> (`.github/workflows/be-ci-cd.yml`, repo kökünde) yalnızca `halisaha-be/**`
> altında değişiklik olan push/PR'larda tetiklenir; frontend'e dokunan
> değişiklikler bu pipeline'ı çalıştırmaz.

Mimarí:
- **EC2 (t2.micro/t3.micro, Ubuntu 24.04)**: Spring Boot uygulamasını Docker
  container olarak çalıştırır.
- **RDS (db.t3.micro/db.t4g.micro, PostgreSQL)**: Veritabanı, Free Tier
  kapsamında ayrı bir yönetilen servis olarak çalışır.
- **GHCR (GitHub Container Registry)**: Docker imajlarının push edildiği yer.
- **GitHub Actions**: `main`'e her push'ta build → test → image push → EC2'ye
  SSH ile deploy.
- Erişim düz `http://<EC2-IP>:8080` üzerinden (domain/HTTPS yok, istenirse
  sonradan Nginx + Let's Encrypt eklenebilir).

---

## 1) AWS RDS (PostgreSQL) oluşturma

1. AWS Console → **RDS** → **Create database**.
2. **Standard create** seçin.
3. Engine: **PostgreSQL** (Free tier ile uyumlu en güncel 16.x sürümü).
4. Templates: **Free tier**.
5. Settings:
   - DB instance identifier: `halisaha-db`
   - Master username: `halisaha_app` (veya istediğiniz bir kullanıcı adı)
   - Master password: güçlü bir şifre belirleyin, not edin.
6. Instance configuration: `db.t3.micro` (veya bölgenizde free-tier eligible
   olan `db.t4g.micro`).
7. Storage: 20 GiB, gp2/gp3 (free tier limiti).
8. Connectivity:
   - VPC: EC2 ile **aynı** VPC (genelde default VPC).
   - Public access: **No** (güvenlik için önerilir; EC2 aynı VPC'de olduğu
     için erişebilecek).
   - VPC security group: **Create new** → `halisaha-db-sg`.
9. Additional configuration → Initial database name: `halisaha`.
10. **Create database**. Oluşturma birkaç dakika sürer.
11. Oluştuktan sonra **Endpoint** değerini not edin
    (örn: `halisaha-db.xxxxxxxxxx.eu-central-1.rds.amazonaws.com`).

> Not: RDS'in security group'una (`halisaha-db-sg`), EC2 oluşturduktan sonra
> EC2'nin security group'undan **5432** portuna izin vereceğiz (adım 3).

---

## 2) AWS EC2 instance oluşturma

1. AWS Console → **EC2** → **Launch instance**.
2. Name: `halisaha-app`.
3. AMI: **Ubuntu Server 24.04 LTS** (Free tier eligible).
4. Instance type: `t2.micro` (veya bölgenizde free-tier eligible `t3.micro`).
5. Key pair: **Create new key pair**
   - Name: `halisaha-key`
   - Type: RSA, format: `.pem`
   - İndirilen `halisaha-key.pem` dosyasını güvenli bir yerde saklayın —
     tekrar indirilemez. Bu, sunucuya SSH ile bağlanmak ve **GitHub
     Actions'ın deploy yapabilmesi için** gereken private key'dir.
6. Network settings → Edit:
   - VPC: RDS ile aynı VPC.
   - Security group: **Create new** → `halisaha-app-sg`, kurallar:
     - SSH (22) → Source: `0.0.0.0/0`. GitHub Actions'ın hosted runner'ları
       sabit bir IP aralığından gelmediği için deploy adımının bağlanabilmesi
       adına bu port açık olmak zorunda. Güvenliği key-only authentication
       (EC2'de varsayılan, şifre ile giriş kapalı) sağlıyor; ek önlem olarak
       sunucuya kurulumdan sonra `fail2ban` kurmanız önerilir
       (`sudo apt-get install -y fail2ban`).
     - Custom TCP (8080) → Source: `0.0.0.0/0` (uygulamaya dışarıdan erişim).
7. Storage: varsayılan 8-30 GiB gp3 (free tier limiti 30 GiB).
8. **Launch instance**.
9. Instance çalışmaya başladıktan sonra **Elastic IP** ayırıp bu instance'a
   bağlayın (EC2 → Elastic IPs → Allocate → Associate). Bu, instance
   restart olsa bile IP'nin sabit kalmasını sağlar (Free tier: instance'a
   bağlıyken ücretsiz).
10. Elastic IP adresini not edin — bu, `EC2_HOST` secret'ı olacak.

### RDS security group'unu EC2'ye açma

1. EC2 → Security Groups → `halisaha-app-sg`'nin ID'sini kopyalayın.
2. RDS → Security Groups → `halisaha-db-sg` → Edit inbound rules:
   - Type: PostgreSQL (5432) → Source: `halisaha-app-sg` (security group ID
     olarak seçin, IP değil).

---

## 3) EC2 üzerinde Docker kurulumu

Monorepo'nun kökünden çalıştırın (script `halisaha-be/deploy/` altında):

```bash
scp -i halisaha-key.pem halisaha-be/deploy/setup-ec2.sh ubuntu@<ELASTIC_IP>:~/
ssh -i halisaha-key.pem ubuntu@<ELASTIC_IP>
chmod +x setup-ec2.sh && ./setup-ec2.sh
```

Script tamamlandıktan sonra oturumu kapatıp tekrar SSH ile bağlanın (docker
grubu üyeliğinin aktif olması için).

---

## 4) Sunucuda `.env` dosyasını oluşturma

`deploy/.env.example` dosyasını referans alarak sunucuda
`/opt/halisaha-be/.env` dosyasını oluşturun:

```bash
ssh -i halisaha-key.pem ubuntu@<ELASTIC_IP>
mkdir -p /opt/halisaha-be
nano /opt/halisaha-be/.env
```

İçeriği doldururken:
- `DB_URL`: `jdbc:postgresql://<RDS-ENDPOINT>:5432/halisaha`
- `DB_USERNAME` / `DB_PASSWORD`: RDS'i oluştururken belirlediğiniz bilgiler.
- `JWT_SECRET`: `openssl rand -base64 48` ile üretin.
- `CORS_ALLOWED_ORIGINS`: Frontend'in gerçek adresi.

Bu dosya **asla git'e commit edilmemeli**; sadece sunucuda durur ve CI/CD
tarafından dokunulmaz.

---

## 5) GHCR (GitHub Container Registry) imajını public yapma

CI/CD ilk kez `main`'e push edildiğinde imajı otomatik olarak
`ghcr.io/<owner>/halisaha-be` altına push edecek (repo adı `halisaha-app`
olsa da imaj paketi ayrı bir isimle, `halisaha-be`, yayınlanır — ileride
frontend için `ghcr.io/<owner>/halisaha-fe` ayrı bir paket olacak). EC2'nin
bu imajı **login olmadan** `docker compose pull` ile çekebilmesi için
paketi public yapmanız gerekiyor (en basit yol):

1. İlk push sonrası GitHub → profiliniz → **Packages** →
   `halisaha-be` paketine girin.
2. **Package settings** → **Change visibility** → **Public**.

> Alternatif: Paketi private tutmak isterseniz, `read:packages` yetkili bir
> Personal Access Token oluşturup `GHCR_PAT` adında bir secret ekleyin ve
> deploy adımında sunucuda `docker login ghcr.io -u <kullanici> -p
> $GHCR_PAT` çalıştıracak şekilde workflow'u güncelleyin. Basitlik için
> varsayılan yaklaşım public paket kullanmaktır.

---

## 6) GitHub repository secrets

Repo → **Settings** → **Secrets and variables** → **Actions** → **New
repository secret**:

| Secret adı       | Değer                                                        |
|------------------|---------------------------------------------------------------|
| `EC2_HOST`       | EC2 Elastic IP adresi                                          |
| `EC2_SSH_USER`   | `ubuntu`                                                       |
| `EC2_SSH_KEY`    | `halisaha-key.pem` dosyasının **tüm içeriği** (private key)    |

`GITHUB_TOKEN` otomatik sağlanır, ekstra bir şey yapmanıza gerek yok.

Ayrıca **Settings → Environments** altında `production` adında bir
environment oluşturmanız önerilir (workflow'daki `environment: production`
alanıyla eşleşir); isterseniz buraya manuel onay (required reviewers) da
ekleyebilirsiniz.

---

## 7) İlk deploy

Her şey hazır olduğunda:

```bash
git push origin main
```

GitHub Actions sekmesinden pipeline'ı izleyin: `test` → `build-and-push` →
`deploy`. Tamamlandığında `http://<ELASTIC_IP>:8080` üzerinden API'ye
erişebilirsiniz.

---

## Sorun giderme

- **Deploy adımı SSH ile bağlanamıyor**: `EC2_SSH_KEY` secret'ının
  `.pem` dosyasının tam içeriği olduğundan (başında/sonunda fazladan
  boşluk/satır olmadan) ve `EC2_SSH_USER` değerinin `ubuntu` olduğundan emin
  olun.
- **`docker compose pull` imajı bulamıyor**: Paketin public olduğundan ve
  imaj adının `ghcr.io/<owner>/halisaha-be` ile birebir eştiğinden emin olun
  (küçük harf olmalı).
- **Uygulama RDS'e bağlanamıyor**: `halisaha-db-sg` inbound kuralında source
  olarak EC2'nin security group ID'sinin (IP değil) seçildiğinden emin olun.
