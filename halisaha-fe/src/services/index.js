import { api, tokenStore } from '@/lib/api';

/* ── /api/auth ─────────────────────────────────────────────── */
export const authService = {
  login: (kullaniciAdi, sifre) =>
    api.post('/api/auth/login', { kullaniciAdi, sifre }).then((r) => r.data),
  refresh: (refreshToken) =>
    api.post('/api/auth/refresh', { refreshToken }).then((r) => r.data),
  /** Canlılık sinyali: tarayıcının açık olduğunu bildirir (sessizce; hata yutulur). */
  aktif: () => {
    const refreshToken = tokenStore.refresh();
    if (!refreshToken) return Promise.resolve();
    return api.post('/api/auth/aktif', { refreshToken }).catch(() => {});
  },
  logout: () => {
    const refreshToken = tokenStore.refresh();
    if (!refreshToken) return Promise.resolve();
    return api.post('/api/auth/logout', { refreshToken }).catch(() => {});
  },
  me: () => api.get('/api/auth/me').then((r) => r.data),
  sifreDegistir: (eskiSifre, yeniSifre) =>
    api.post('/api/auth/sifre-degistir', { eskiSifre, yeniSifre }),
};

/* ── /api/kullanicilar (yalnızca admin) ───────────────────────── */
export const kullaniciService = {
  list: () => api.get('/api/kullanicilar').then((r) => r.data),
  get: (id) => api.get('/api/kullanicilar/' + id).then((r) => r.data),
  create: (body) => api.post('/api/kullanicilar', body).then((r) => r.data),
  /** body: { adSoyad, sifre? } — sifre boş/yok ise değişmez */
  guncelle: (id, body) => api.put('/api/kullanicilar/' + id, body).then((r) => r.data),
  rolGuncelle: (id, rol) => api.put('/api/kullanicilar/' + id + '/rol', { rol }).then((r) => r.data),
  yetkileriGuncelle: (id, yetkiler) =>
    api.put('/api/kullanicilar/' + id + '/yetkiler', { yetkiler }).then((r) => r.data),
  /** body: { grupKisitli, grupIdleri } — kısıtsızsa personel tüm grupları görür */
  grupYetkisiniGuncelle: (id, body) => api.put('/api/kullanicilar/' + id + '/gruplar', body).then((r) => r.data),
  durumGuncelle: (id, durum) =>
    api.patch('/api/kullanicilar/' + id + '/durum', null, { params: { durum } }).then((r) => r.data),
};

/* ── /api/isletmeler (yalnızca SUPER_ADMIN) ───────────────────── */
export const isletmeService = {
  list: () => api.get('/api/isletmeler').then((r) => r.data),
  get: (id) => api.get('/api/isletmeler/' + id).then((r) => r.data),
  /** body: { ad, yonetici: { adSoyad, kullaniciAdi, sifre } } — işletme ve ilk admini birlikte oluşturulur */
  create: (body) => api.post('/api/isletmeler', body).then((r) => r.data),
  update: (id, body) => api.put('/api/isletmeler/' + id, body).then((r) => r.data),
  durumGuncelle: (id, durum) =>
    api.patch('/api/isletmeler/' + id + '/durum', null, { params: { durum } }).then((r) => r.data),
  kullanicilar: (id) => api.get('/api/isletmeler/' + id + '/kullanicilar').then((r) => r.data),
  kullaniciOlustur: (id, body) =>
    api.post('/api/isletmeler/' + id + '/kullanicilar', body).then((r) => r.data),
  kullaniciDurum: (id, kullaniciId, durum) =>
    api
      .patch('/api/isletmeler/' + id + '/kullanicilar/' + kullaniciId + '/durum', null, { params: { durum } })
      .then((r) => r.data),
  sifreSifirla: (id, kullaniciId, sifre) =>
    api.put('/api/isletmeler/' + id + '/kullanicilar/' + kullaniciId + '/sifre', { sifre }),
};

/* ── /api/sahalar ──────────────────────────────────────────── */
export const sahaService = {
  list: () => api.get('/api/sahalar').then((r) => r.data),
  get: (id) => api.get('/api/sahalar/' + id).then((r) => r.data),
  create: (body) => api.post('/api/sahalar', body).then((r) => r.data),
  update: (id, body) => api.put('/api/sahalar/' + id, body).then((r) => r.data),
  remove: (id) => api.delete('/api/sahalar/' + id),
};

/* ── /api/hesaplar (okuma herkese, yazma ADMIN ya da HESAP yetkisi) ── */
export const hesapService = {
  list: () => api.get('/api/hesaplar').then((r) => r.data),
  get: (id) => api.get('/api/hesaplar/' + id).then((r) => r.data),
  create: (body) => api.post('/api/hesaplar', body).then((r) => r.data),
  update: (id, body) => api.put('/api/hesaplar/' + id, body).then((r) => r.data),
  remove: (id) => api.delete('/api/hesaplar/' + id),
};

/* ── /api/rezervasyonlar ───────────────────────────────────── */
export const rezervasyonService = {
  list: (params) => api.get('/api/rezervasyonlar', { params }).then((r) => r.data),
  get: (id) => api.get('/api/rezervasyonlar/' + id).then((r) => r.data),
  create: (body) => api.post('/api/rezervasyonlar', body).then((r) => r.data),
  update: (id, body) => api.put('/api/rezervasyonlar/' + id, body).then((r) => r.data),
  /** Kayıttan sonra ödenecek (saha) tutarı değiştirir; alınan ödemenin altına düşemez. Güncel rezervasyonu döner */
  sahaTutari: (id, tutar) => api.patch('/api/rezervasyonlar/' + id + '/saha-tutari', { tutar }).then((r) => r.data),
  /** neden isteğe bağlı; iptal eden kullanıcı ve zaman sunucuda kaydedilir */
  iptal: (id, neden) => api.patch('/api/rezervasyonlar/' + id + '/iptal', { neden: neden || null }).then((r) => r.data),
  remove: (id) => api.delete('/api/rezervasyonlar/' + id),
  urunEkle: (id, urunId, miktar) =>
    api.post('/api/rezervasyonlar/' + id + '/urunler', { urunId, miktar }).then((r) => r.data),
  urunCikar: (id, rezervasyonUrunId) =>
    api
      .delete('/api/rezervasyonlar/' + id + '/urunler/' + rezervasyonUrunId)
      .then((r) => r.data),
  /** body: { tip: 'KAPORA' | 'ODEME', tutar, yontem } */
  odemeEkle: (id, body) =>
    api.post('/api/rezervasyonlar/' + id + '/odemeler', body).then((r) => r.data),
};

/* ── /api/raporlar (sadece ADMIN) ──────────────────────────── */
export const raporService = {
  /** params: { baslangic, bitis, sahaId, grupla: GUN|HAFTA|AY, esas: ODEME|SEANS } */
  ciro: (params) => api.get('/api/raporlar/ciro', { params }).then((r) => r.data),
  /** params: { baslangic, bitis } (ISO tarih) — varsayılan: bu ayın başından bugüne */
  finans: (params) => api.get('/api/raporlar/finans', { params }).then((r) => r.data),
  /** params: { yil, ay } — seçilen ayın öğrenci ödeme tahsilat özeti */
  ogrenci: (params) => api.get('/api/raporlar/ogrenci', { params }).then((r) => r.data),
  /** params: { baslangic, bitis } — birden fazla aya yayılan dönemin ay ay öğrenci ödeme tahsilat özeti */
  ogrenciAylik: (params) => api.get('/api/raporlar/ogrenci/aylik', { params }).then((r) => r.data),
  yoklama: (params) => api.get('/api/raporlar/yoklama', { params }).then((r) => r.data),
  rezervasyon: (params) => api.get('/api/raporlar/rezervasyon', { params }).then((r) => r.data),
  envanter: (params) => api.get('/api/raporlar/envanter', { params }).then((r) => r.data),
  /** Kullanıcı sekmesi — hepsi params: { baslangic, bitis } */
  kullaniciKasa: (params) => api.get('/api/raporlar/kullanici/kasa', { params }).then((r) => r.data),
  kullaniciIslemler: (params) => api.get('/api/raporlar/kullanici/islemler', { params }).then((r) => r.data),
  kullaniciIadeler: (params) => api.get('/api/raporlar/kullanici/iadeler', { params }).then((r) => r.data),
  kullaniciIptaller: (params) => api.get('/api/raporlar/kullanici/iptaller', { params }).then((r) => r.data),
  kullaniciYoklama: (params) => api.get('/api/raporlar/kullanici/yoklama', { params }).then((r) => r.data),
  /** + kullaniciId (isteğe bağlı): giriş / çıkış / başarısız giriş geçmişi */
  kullaniciGiris: (params) => api.get('/api/raporlar/kullanici/giris', { params }).then((r) => r.data),
};

/* ── /api/ogrenciler ───────────────────────────────────────── */
export const ogrenciService = {
  /** params: { grupId, q, odemeBekleyen, envanterEksik, sayfa (0 tabanlı), boyut } — sayfalı yanıt { icerik, sayfa, boyut, toplamKayit, toplamSayfa } döner */
  list: (params) => api.get('/api/ogrenciler', { params }).then((r) => r.data),
  get: (id) => api.get('/api/ogrenciler/' + id).then((r) => r.data),
  create: (body) => api.post('/api/ogrenciler', body).then((r) => r.data),
  update: (id, body) => api.put('/api/ogrenciler/' + id, body).then((r) => r.data),
  remove: (id) => api.delete('/api/ogrenciler/' + id),
};

/* ── /api/ogrenciler/{id}/yoklamalar (öğrenci detayındaki yoklama bilgileri) ── */
export const ogrenciYoklamaService = {
  /** { sayfa: { icerik: [{ tarih, durum: GELDI|GELMEDI|IZINLI, not, kaydeden }], sayfa, boyut, toplamKayit, toplamSayfa }, geldi, gelmedi, izinli } — tarihe göre yeniden eskiye */
  gecmis: (ogrenciId, params) => api.get('/api/ogrenciler/' + ogrenciId + '/yoklamalar', { params }).then((r) => r.data),
};

/* ── /api/ogrenciler/{id}/odemeler (öğrenci detayındaki aylık ödemeleri) ── */
export const ogrenciOdemeService = {
  /** { yil, bugun (sunucu tarihi), aylar: [{ yil, ay, odenenTutar, durum (ODENDI | ODENMEDI), odemeSekli, hesapId, hesapAd, odemeTarihi, not }] (12 ay), toplamOdenen, yillar } */
  getir: (ogrenciId, yil) => api.get('/api/ogrenciler/' + ogrenciId + '/odemeler/' + yil).then((r) => r.data),
  /** body: { aylar: [{ ay, odenenTutar, odemeSekli, hesapId, odemeTarihi }] } — yalnızca gönderilen aylar kaydedilir; tutar 0/boş ise o ayın kaydı silinir */
  kaydet: (ogrenciId, yil, body) =>
    api.put('/api/ogrenciler/' + ogrenciId + '/odemeler/' + yil, body).then((r) => r.data),
};

/* ── Öğrenci grup transferi ───────────────────────────────────── */
export const transferService = {
  /** Öğrencinin transfer geçmişi (en yeni önce): [{ id, kaynakGrupAd, hedefGrupAd, durum (BEKLIYOR | ONAYLANDI | REDDEDILDI | IPTAL), aciklama, talepEden, talepTarihi, kararVeren, kararTarihi, kararNotu }] */
  gecmis: (ogrenciId) => api.get('/api/ogrenciler/' + ogrenciId + '/transferler').then((r) => r.data),
  /** body: { hedefGrupId, aciklama? } — ADMIN ya da OGRENCI yetkisi; admin onaylayana kadar öğrenci eski grubunda kalır */
  talepEt: (ogrenciId, body) => api.post('/api/ogrenciler/' + ogrenciId + '/transferler', body).then((r) => r.data),
  /** body: { hedefGrupId, aciklama? } — yalnızca bekleyen talep güncellenebilir */
  guncelle: (id, body) => api.put('/api/transferler/' + id, body).then((r) => r.data),
  /** Bekleyen talebi geri çeker (ADMIN ya da OGRENCI yetkisi) */
  iptal: (id, not) => api.put('/api/transferler/' + id + '/iptal', { not: not || null }).then((r) => r.data),
  /** Yalnızca admin: durum boşsa tüm talepler (BEKLIYOR | ONAYLANDI | REDDEDILDI | IPTAL) */
  list: (durum) => api.get('/api/transferler', { params: durum ? { durum } : {} }).then((r) => r.data),
  bekleyenSayisi: () => api.get('/api/transferler/bekleyen-sayisi').then((r) => r.data.sayi),
  onayla: (id) => api.put('/api/transferler/' + id + '/onayla').then((r) => r.data),
  reddet: (id, not) => api.put('/api/transferler/' + id + '/reddet', { not: not || null }).then((r) => r.data),
};

/* ── /api/yoklamalar (ADMIN ya da YOKLAMA yetkisi) ───────────── */
export const yoklamaService = {
  /** Grubun aktif öğrencileri ve seçili günkü yoklamaları: [{ ogrenciId, adSoyad, durum: GELDI|GELMEDI|IZINLI|null, not }] */
  list: (tarih, grupId) => api.get('/api/yoklamalar', { params: { tarih, grupId } }).then((r) => r.data),
  /** Tüm satırları tek seferde kaydeder; durum null olan öğrencinin kaydı silinir. Güncel listeyi döner. */
  kaydet: (tarih, grupId, kayitlar) =>
    api.put('/api/yoklamalar', { kayitlar }, { params: { tarih, grupId } }).then((r) => r.data),
};

/* ── /api/envanterler (ADMIN ya da ENVANTER yetkisi) ───────────── */
export const envanterService = {
  /** [{ id, tur, turAd, bedenli, beden, adet, stokUyariAdedi, satisFiyati }] — tür (alfabetik) ve beden sırasıyla */
  list: () => api.get('/api/envanterler').then((r) => r.data),
  get: (id) => api.get('/api/envanterler/' + id).then((r) => r.data),
  /** Panel kartı: { kalemSayisi, ogrenciler: [{ ogrenciId, adSoyad, kalemler: [{ turAd, beden, bedenli, stokYok }] }] } — ödemesi alınmış, teslim edilmemiş */
  teslimBekleyenler: () => api.get('/api/envanterler/teslim-bekleyenler').then((r) => r.data),
  /** body: { tur, beden (yalnızca bedenli türlerde), adet, satisFiyati, stokUyariAdedi } */
  create: (body) => api.post('/api/envanterler', body).then((r) => r.data),
  update: (id, body) => api.put('/api/envanterler/' + id, body).then((r) => r.data),
  remove: (id) => api.delete('/api/envanterler/' + id),
};

/* ── /api/envanter-setleri (ADMIN ya da ENVANTER yetkisi) ───────── */
export const envanterSetService = {
  /** [{ id, ad, beden, tutar, kayitTarihi, guncellemeTarihi, kaydeden, kalemler: [{ envanterId, tur, turAd, beden, adet }] }] — ada göre */
  list: () => api.get('/api/envanter-setleri').then((r) => r.data),
  get: (id) => api.get('/api/envanter-setleri/' + id).then((r) => r.data),
  /** body: { ad, beden, tutar, envanterIdleri: [id] } — seçilen envanterler; bedenliler set bedeninde olmalı, her türden en fazla bir tane, stoksuz ürün 400 */
  create: (body) => api.post('/api/envanter-setleri', body).then((r) => r.data),
  update: (id, body) => api.put('/api/envanter-setleri/' + id, body).then((r) => r.data),
  remove: (id) => api.delete('/api/envanter-setleri/' + id),
};

/* ── /api/ogrenciler/{id}/envanter-satislari (öğrenci detayındaki envanter seti satışı) ── */
export const envanterSatisService = {
  list: (ogrenciId) => api.get('/api/ogrenciler/' + ogrenciId + '/envanter-satislari').then((r) => r.data),
  /**
   * body: { kalemler: [{ tur, beden, odendi, teslim }] (her türden bir tane), odenenTutar (kaydedildikten sonraki TOPLAM ödenen),
   * odemeSekli: NAKIT | KART | HAVALE_EFT ve hesapId (yalnızca HAVALE_EFT) — toplam artıyorsa aradaki fark bu yöntemle yeni ödeme olur,
   * setId (isteğe bağlı; yalnızca oluştururken) }. Yanıttaki odemeler: [{ id, tutar, odemeSekli, hesapId, hesapAd, odemeTarihi }]
   */
  create: (ogrenciId, body) =>
    api.post('/api/ogrenciler/' + ogrenciId + '/envanter-satislari', body).then((r) => r.data),
  update: (ogrenciId, satisId, body) =>
    api.put('/api/ogrenciler/' + ogrenciId + '/envanter-satislari/' + satisId, body).then((r) => r.data),
  remove: (ogrenciId, satisId) =>
    api.delete('/api/ogrenciler/' + ogrenciId + '/envanter-satislari/' + satisId),
  /** Yanlış girilen bir ödemeyi siler (satışın en az bir ödemesi kalmalı); güncel satışı döner */
  odemeSil: (ogrenciId, satisId, odemeId) =>
    api.delete('/api/ogrenciler/' + ogrenciId + '/envanter-satislari/' + satisId + '/odemeler/' + odemeId).then((r) => r.data),
};

/* ── /api/abonelikler (haftalık sabit rezervasyon: Aboneler menüsü) ── */
export const aboneService = {
  /** params: { durum: AKTIF | IPTAL, q } — durum boşsa hepsi */
  list: (params) => api.get('/api/abonelikler', { params }).then((r) => r.data),
  /** Detay: ödemeler ve haftalık rezervasyon listesi (haftalar) dahil */
  get: (id) => api.get('/api/abonelikler/' + id).then((r) => r.data),
  /** { standartUcret } — tanımlı değilse null */
  ayar: () => api.get('/api/abonelikler/ayar').then((r) => r.data),
  /** body: { standartUcret } — yalnızca ADMIN */
  ayarKaydet: (body) => api.put('/api/abonelikler/ayar', body).then((r) => r.data),
  /** body: { sahaId, haftaGunu (1=Pzt..7=Paz), baslangicSaat (12..25), sureSaat, baslangicTarih } — { rezervasyonSayisi, bitisTarih, cakismalar: [{ tarih, adSoyad }] } */
  onizle: (body) => api.post('/api/abonelikler/onizleme', body).then((r) => r.data),
  /** body: { sahaId, adSoyad, telNo (05…), haftaGunu, baslangicDakika, sureDakika, baslangicTarih, depozito, haftalikUcret, odenenTutar (alınan depozito), odemeSekli, hesapId, odemeTarihi, not } — bitiş tarihi sunucuda 52. haftanın tarihi olarak hesaplanır */
  create: (body) => api.post('/api/abonelikler', body).then((r) => r.data),
  /** body: { adSoyad, telNo, depozito, haftalikUcret, not } — haftalık ücret, ödemesi alınmamış gelecek haftalara da yansır */
  update: (id, body) => api.put('/api/abonelikler/' + id, body).then((r) => r.data),
  /** İptal edilirse ne olacağı: { kalanDepozito, haftalikUcret, oynanacakHafta, iptalEdilecekHafta, artanTutar, kismiHaftaVar, kismiHaftaOdenecek, iadeZorunlu } */
  iptalOnizle: (id) => api.get('/api/abonelikler/' + id + '/iptal-onizleme').then((r) => r.data),
  /** body: { neden, artanIade (artan depozito iade edilsin), iadeSekli, hesapId } — kalan depozito kadar hafta oynanır, diğer haftalar iptal edilir */
  iptal: (id, body) => api.put('/api/abonelikler/' + id + '/iptal', body).then((r) => r.data),
  /** Depozito ödemesi. body: { tutar, odemeSekli, hesapId, odemeTarihi } */
  odemeEkle: (id, body) => api.post('/api/abonelikler/' + id + '/odemeler', body).then((r) => r.data),
  odemeSil: (id, odemeId) => api.delete('/api/abonelikler/' + id + '/odemeler/' + odemeId).then((r) => r.data),
};

/* ── /api/iadeler (alınan ödemenin geri verilmesi; rezervasyon, öğrenci aylık ödemesi, envanter satışı, abonelik) ── */
export const iadeService = {
  /** params: tam olarak biri { rezervasyonId | ogrenciOdemeId | envanterSatisId | abonelikId } ya da { ogrenciId, yil } (öğrencinin yılın tüm iadeleri) */
  list: (params) => api.get('/api/iadeler', { params }).then((r) => r.data),
  /** body: { rezervasyonId | ogrenciOdemeId | envanterSatisId | abonelikId (biri), tutar, iadeSekli, hesapId, iadeTarihi, neden } — yalnızca ADMIN */
  yap: (body) => api.post('/api/iadeler', body).then((r) => r.data),
  /** Hatalı girilen iade kaydını siler — yalnızca ADMIN */
  remove: (id) => api.delete('/api/iadeler/' + id),
};

/* ── /api/gruplar ──────────────────────────────────────────── */
export const grupService = {
  list: () => api.get('/api/gruplar').then((r) => r.data),
  /** Transfer hedefi için tüm gruplar (grup yetkisinden bağımsız) */
  hedefler: () => api.get('/api/gruplar/hedefler').then((r) => r.data),
  get: (id) => api.get('/api/gruplar/' + id).then((r) => r.data),
  create: (body) => api.post('/api/gruplar', body).then((r) => r.data),
  update: (id, body) => api.put('/api/gruplar/' + id, body).then((r) => r.data),
  remove: (id) => api.delete('/api/gruplar/' + id),
};

/* ── /api/urunler ──────────────────────────────────────────── */
export const urunService = {
  list: () => api.get('/api/urunler').then((r) => r.data),
  get: (id) => api.get('/api/urunler/' + id).then((r) => r.data),
  create: (body) => api.post('/api/urunler', body).then((r) => r.data),
  update: (id, body) => api.put('/api/urunler/' + id, body).then((r) => r.data),
  remove: (id) => api.delete('/api/urunler/' + id),
};
