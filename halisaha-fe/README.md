# halisaha-fe

Halı Saha Yönetim Sistemi arayüzü. `halisaha-be` (Spring Boot) API'sine bağlanır.

## Teknoloji

React 18 · Vite · Tailwind CSS v4 · Axios · React Router · lucide-react · recharts

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

Giriş bilgileri: ilk açılışta backend bir süper admin oluşturur (kullanıcı adı ve şifre `SUPER_ADMIN_USERNAME` /
`SUPER_ADMIN_PASSWORD` ortam değişkenlerinden gelir; şifre verilmezse süper admin oluşturulmaz. Yerelde backend'i
`SUPER_ADMIN_PASSWORD=<şifre>` ile başlatın). İşletmeler ve işletme admin kullanıcıları süper admin
ekranından oluşturulur. Hiçbir ortamda ortak/varsayılan bir şifre kullanmayın.

## Çoklu işletme

Her işletmenin sahası, ürünü, öğrencisi, grubu, rezervasyonu ve kullanıcıları ayrıdır; kullanıcı yalnızca kendi
işletmesinin verisini görür (backend `isletme_id` ile filtreler, frontend'de ayrıca bir şey gerekmez).
`SUPER_ADMIN` bir işletmeye bağlı değildir: menüde yalnızca **İşletmeler** görünür, işletme verisine erişemez.
Üst bardaki kullanıcı adının yanında işletme adı gösterilir.

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
| `/yoklama` | Öğrenci yoklaması: tarih + grup seç, her öğrenci için Geldi / Gelmedi / İzinli ve not (yalnızca ADMIN ya da `YOKLAMA` yetkisi) | `GET /api/gruplar`, `GET /api/yoklamalar?tarih=&grupId=`, `PUT /api/yoklamalar?tarih=&grupId=` |
| `/envanter` | Envanter listesi (tür alfabetik, beden sırasıyla; Stok az / Tükendi etiketleri) ve **Setler** sekmesi (`?sekme=setler`: ad, içerdiği envanterler, toplam tutar). Yalnızca ADMIN ya da `ENVANTER` yetkisi | `GET /api/envanterler`, `DELETE /api/envanterler/{id}`, `GET /api/envanter-setleri`, `DELETE /api/envanter-setleri/{id}` |
| `/envanter/set/yeni`, `/envanter/set/:id` | Set ekle / güncelle: ad + kayıtlı envanterler (tür + beden, her türden en fazla bir), seçildikçe toplam tutar | `POST /api/envanter-setleri`, `GET/PUT /api/envanter-setleri/{id}` |
| `/envanter/yeni` | Yeni envanter: tür, beden (yalnızca Kışlık Eşofman / Yazlık Forma / Yağmurluk için: 128, 140, 152, 164, 176, Small, Medium), satış fiyatı, adet, stok uyarı adedi | `POST /api/envanterler` |
| `/envanter/:id` | Envanter güncelle (tür ve beden de değişebilir; satışta kullanılıyorsa değişemez) | `GET/PUT /api/envanterler/{id}` |
| `/ogrenciler/:id` | **Transfer bilgileri** kartı: hedef grup seçilip transfer talebi açılır (admin onaylayana kadar öğrenci eski grubunda kalır), bekleyen talep güncellenebilir ya da geri çekilebilir; altta nereden nereye / ne zaman / kim talep etti-onayladı geçmişi. Öğrencinin grubu varsa Grup alanı salt okunur, değişiklik transferle yapılır | `GET/POST /api/ogrenciler/{id}/transferler`, `PUT /api/transferler/{id}/iptal` |
| `/transferler` | (Admin ya da Transferler yetkisi) Öğrenci transfer talepleri: Bekleyenler / Tümü; **Onayla** (öğrenci hemen yeni gruba geçer) ya da **Reddet** (isteğe bağlı red nedeniyle) | `GET /api/transferler`, `PUT /api/transferler/{id}/onayla`, `PUT .../reddet` |
| `/aboneler` | (Admin ya da Aboneler yetkisi) Abonelik listesi: Aktif / İptal / Tümü, ad-telefon arama; üstte **standart abonelik ücreti** (yalnızca admin değiştirir, kayıt formunda ücreti doldurur) | `GET /api/abonelikler`, `GET/PUT /api/abonelikler/ayar` |
| `/aboneler/yeni` | Yeni abonelik: ad soyad, telefon, saha, başlangıç tarihi (bitiş otomatik 1 yıl sonrası, değiştirilemez), haftanın günü / saat / süre; seçime göre **oluşacak haftalık rezervasyon sayısı ve çakışan tarihler** canlı gösterilir (çakışma varsa kaydedilemez); abonelik ücreti (standart ücretten gelir, elle değişir), alınan ödeme, ödeme şekli (Nakit/Kart/Havale+hesap), ödeme tarihi. Kayıtta her hafta için bir rezervasyon oluşur; takvimde yeşil **Abone** bloğu olarak görünür | `POST /api/abonelikler/onizleme`, `POST /api/abonelikler` |
| `/aboneler/:id` | Abonelik detayı: rezervasyon bilgileri, abone bilgisi (ad / telefon / ücret / not güncellenir), ödemeler (yeni ödeme ekle, yanlış ödemeyi sil), haftalık rezervasyon listesi, **Aboneliği iptal et** (gelecekteki haftalık rezervasyonlar iptal edilir, ödemeler silinmez) | `GET/PUT /api/abonelikler/{id}`, `PUT .../iptal`, `POST/DELETE .../odemeler` |
| (iade) | Rezervasyon detayı, abone detayı, öğrenci detayındaki aylık ödemeler ve envanter satışı güncelleme ekranında **İadeler** bölümü: yapılan iadeler listelenir; yönetici **İade yap** (tutar ≤ iade edilebilen, iade şekli, tarih, neden) ya da hatalı iadeyi **Sil**. İade ödeme kaydını/durumunu değiştirmez, raporlarda iade tarihinde tahsilattan düşülür | `GET /api/iadeler`, `POST /api/iadeler`, `DELETE /api/iadeler/{id}` |
| `/raporlar` | (Yalnızca ADMIN; Personel yetki listesinde yoktur) Finans (tahsil edilen / bekleyen ciro, trend, kaynak ve yöntem dağılımı), Öğrenci (tek ay seçiliyse o ayın ödeme tahsilat oranı ve grup bazı; birden fazla aya yayılan dönemde – ör. Bu yıl – ay ay tahsilat/oran grafikleri, ay ay detay ve grup bazında özet), Yoklama (devam oranları), Rezervasyon (saha doluluğu, yoğun saat/gün), Envanter (satışlar, kritik stok) sekmeleri; Bugün / Bu hafta / Bu ay / Geçen ay / Bu yıl / Özel tarih filtresi; grafikler recharts. Her işletme yalnızca kendi verisini görür | `GET /api/raporlar/finans`, `/ogrenci`, `/ogrenci/aylik`, `/yoklama`, `/rezervasyon`, `/envanter` |
| `/ogrenciler/:id` | (Envanter yetkisi olanlar için) **Envanter bilgileri** kartı (her ödeme ayrı satır olarak tarih / tutar / ödeme şekli (Nakit-Kart-Havale+hesap) ile tutulur; satış güncellenirken geçmiş ödemeler listelenir, yeni alınan ödeme **Ek ödeme** olarak girilir, yanlış ödeme silinebilir): öğrenciye envanter seti satışı; **Set** combobox'ından set seçilince tablo setin envanterleriyle dolar (bedenler değiştirilebilir; seçilmezse kayıtlı envanterlerden özel satış). Her envanter için Ödendi / Teslim; Ödendi işaretlenenlerin fiyatları ödenen tutara eklenir; stok yalnızca teslimde düşer | `GET/POST /api/ogrenciler/{id}/envanter-satislari`, `DELETE .../{satisId}/odemeler/{odemeId}`, `PUT/DELETE .../{satisId}` |
| `/sahalar` | Saha listesi | `GET /api/sahalar`, `DELETE /api/sahalar/{id}` |
| `/sahalar/yeni` | Saha kaydı | `POST /api/sahalar` |
| `/urunler` | Kafeterya ürünleri (stok ±) | `GET /api/urunler`, `PUT /api/urunler/{id}`, `DELETE /api/urunler/{id}` |
| `/urunler/yeni` | Ürün kaydı | `POST /api/urunler` |
| `/isletmeler` | İşletme listesi, pasife alma (yalnızca SUPER_ADMIN) | `GET /api/isletmeler`, `PATCH /api/isletmeler/{id}/durum` |
| `/isletmeler/yeni` | İşletme + ilk admini oluşturma | `POST /api/isletmeler` |
| `/isletmeler/:id` | İşletme adı, kullanıcılar, şifre sıfırlama | `GET/PUT /api/isletmeler/{id}`, `GET/POST /api/isletmeler/{id}/kullanicilar`, `PATCH .../kullanicilar/{kid}/durum`, `PUT .../kullanicilar/{kid}/sifre` |

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

- Takvim ızgarası ve rezervasyon formu 12:00–02:00 arasını gösterir (`ACILIS = 12`, `KAPANIS = 26`; 24 ve üzeri
  ertesi günün saatidir, 26 = 02:00). Sabitler `TakvimPage.jsx` ve `RezervasyonYeniPage.jsx` içinde tanımlıdır.
  `DashboardPage.jsx` hâlâ eski 09:00–23:00 sabitlerini kullanıyor.
- Rezervasyon saatleri backend'e `LocalDateTime` formatında (`2026-09-12T19:00:00`) gönderilir.
- `odenecekTutar` backend tarafından hesaplanır; ürün ekleme/çıkarma sonrası dönen response doğrudan kullanılır.
- UI bileşenleri shadcn/ui desenine (cva varyantları + `cn` yardımcısı) göre yazılmıştır; bağımlılık
  olarak shadcn CLI gerekmez, bileşenler `src/components/ui/` altında projeye aittir.

## Mobil uyumluluk (zorunlu)

Uygulama **hem bilgisayardan hem telefondan (iPhone / iOS Safari dahil)** kullanılır. Frontend'de yapılan her
değişiklik telefonda da çalışmalı; bir değişikliği bitmiş saymadan önce şunlar kontrol edilir:

- **390 px genişlikte** (iPhone) açıp bak: yatay kaydırma/taşma olmamalı, butonlar ve form alanları kartın içinde kalmalı.
  Sabit genişlik (`w-[340px]` gibi) yerine `min-w-0`, `flex-1`, `max-w-full`, `sm:` kırılımları kullan.
- **Input yazı boyutu en az 16 px** olmalı; daha küçükse iOS Safari alana odaklanınca sayfayı yakınlaştırır.
- **Tarih/saat seçicilerde** `showPicker()` gibi programatik açmaya güvenme. iOS'ta seçici ancak kullanıcı
  doğrudan `<input type="date">`'e dokununca açılır: input'u görünür alanın üstüne `opacity-0` ile bindir
  (bkz. `TakvimPage.jsx`); `pointer-events-none` verme. `showPicker` sadece masaüstü için ek yardımcıdır.
- **Liste tabloları** dar ekranda yana kaydırılmamalı: `<Table kart>` kullan; hücrelere `etiket="…"` (kart içinde başlık) ve gerekirse `tam` (tüm genişlik) ver. 640 px altında satırlar kart olur, masaüstü değişmez (`index.css` → `.tablo-kart`).
- **Hover'a bağlı etkileşim** olmamalı (dokunmatikte hover yok); dokunma hedefleri yaklaşık 40 px ve üzeri olmalı.
- Bildirimler (toast) telefonda ekranın üstünde tam genişlikte açılır; yeni bildirim eklerken `useToast` / `useHata` kullan.

Test edilen genişlikler: 320, 360 (Android), 390 (iPhone), 412 (Android).

Hızlı doğrulama: dev sunucu açıkken Playwright ile iPhone emülasyonu (`devices['iPhone 13']`, Chromium) kullanıp
her sayfada `document.documentElement.scrollWidth > window.innerWidth` kontrolü ve ekran görüntüsü alınabilir.
Not: emülasyon iOS Safari'nin yerleşik tarih/saat seçicisini ve klavye davranışını birebir göstermez; seçici,
form ve ödeme gibi kritik akışlar yayına almadan önce **gerçek bir iPhone'da** da denenmelidir.
