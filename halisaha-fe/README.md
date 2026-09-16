# halisaha-fe

Halı Saha Yönetim Sistemi arayüzü. `halisaha-be` (Spring Boot) API'sine bağlanır.

## Teknoloji

React 18 · Vite · Tailwind CSS v4 · Axios · React Router · lucide-react

Görsel dil: **Industry** design system (çelik mavisi accent `#5980a6`, açık teknik zemin `#f2f2f3`,
Barlow Condensed başlık / Barlow gövde, kare köşeli "blueprint" kartlar ve nişangah işaretleri).
Tüm token'lar `src/index.css` içindeki `@theme` bloğunda tanımlı — bileşenlerde sabit renk/px yazılmaz.

## Kurulum

```bash
cd halisaha-fe
npm install
cp .env.example .env     # VITE_API_BASE_URL=http://localhost:8080
npm run dev              # http://localhost:5173
```

Backend'in `app.cors.allowed-origins` değeri `http://localhost:5173` içerdiği için ek CORS ayarı gerekmez.

Varsayılan kullanıcı (V2__seed_admin.sql): **admin / admin123**

## Sayfalar (her biri ayrı route)

| Route | Sayfa | Kullandığı endpoint |
| --- | --- | --- |
| `/giris` | Giriş | `POST /api/auth/login` |
| `/panel` | Panel / özet | `GET /api/rezervasyonlar?tarih=`, `GET /api/sahalar`, `GET /api/ogrenciler`, `GET /api/urunler` |
| `/takvim` | Tüm sahalar × saatler gün ızgarası | `GET /api/sahalar`, `GET /api/rezervasyonlar?tarih=` |
| `/rezervasyon/yeni` | Yeni rezervasyon | `GET /api/sahalar`, `POST /api/rezervasyonlar` |
| `/rezervasyon/:id` | Rezervasyon detayı + kafeterya | `GET /api/rezervasyonlar/{id}`, `PUT /api/rezervasyonlar/{id}`, `PATCH /api/rezervasyonlar/{id}/iptal`, `POST /api/rezervasyonlar/{id}/urunler`, `DELETE /api/rezervasyonlar/{id}/urunler/{rezervasyonUrunId}` |
| `/ogrenciler` | Öğrenci listesi (arama) | `GET /api/ogrenciler` |
| `/ogrenciler/yeni` | Öğrenci kaydı | `GET /api/gruplar`, `POST /api/ogrenciler` |
| `/gruplar` | Grup listesi | `GET /api/gruplar`, `DELETE /api/gruplar/{id}` |
| `/gruplar/yeni` | Grup kaydı | `POST /api/gruplar` |
| `/sahalar` | Saha listesi | `GET /api/sahalar`, `DELETE /api/sahalar/{id}` |
| `/sahalar/yeni` | Saha kaydı | `POST /api/sahalar` |
| `/urunler` | Kafeterya ürünleri (stok ±) | `GET /api/urunler`, `PUT /api/urunler/{id}`, `DELETE /api/urunler/{id}` |
| `/urunler/yeni` | Ürün kaydı | `POST /api/urunler` |

## Kimlik doğrulama

`src/lib/api.js` içindeki axios örneği her isteğe `Authorization: Bearer <accessToken>` ekler.
401 dönen ilk istekte `POST /api/auth/refresh` ile token yenilenir, başarısız olursa `/giris`'e yönlendirilir.
Çıkışta `POST /api/auth/logout` çağrılıp refresh token backend'de iptal edilir.
Token'lar `localStorage` içinde `hs_access_token` / `hs_refresh_token` anahtarlarıyla tutulur.

## Klasör yapısı

```
src/
  App.jsx                 route tanımları + korumalı alan
  main.jsx                giriş noktası
  index.css               Tailwind v4 + Industry token'ları + .blueprint
  lib/api.js              axios örneği, token store, interceptor'lar
  lib/format.js           ₺ / tarih / saat / enum etiket yardımcıları
  services/index.js       backend controller'larının birebir karşılığı
  context/AuthContext.jsx login / logout / oturum durumu
  components/AppLayout.jsx üst menü + sayfa başlığı
  components/ui/          Button, Input, Select, Card, Table, Tag, Seg, durum bileşenleri
  pages/                  her ekran ayrı dosya
```

## Notlar

- Takvim ızgarası 09:00–23:00 arasını gösterir; `ACILIS` / `KAPANIS` sabitleri `TakvimPage.jsx` ve
  `DashboardPage.jsx` içinde tanımlıdır.
- Rezervasyon saatleri backend'e `LocalDateTime` formatında (`2026-09-12T19:00:00`) gönderilir.
- `odenecekTutar` backend tarafından hesaplanır; ürün ekleme/çıkarma sonrası dönen response doğrudan kullanılır.
- UI bileşenleri shadcn/ui desenine (cva varyantları + `cn` yardımcısı) göre yazılmıştır; bağımlılık
  olarak shadcn CLI gerekmez, bileşenler `src/components/ui/` altında projeye aittir.
