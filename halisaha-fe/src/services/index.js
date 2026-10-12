import { api, tokenStore } from '@/lib/api';

/* ── /api/auth ─────────────────────────────────────────────── */
export const authService = {
  login: (kullaniciAdi, sifre) =>
    api.post('/api/auth/login', { kullaniciAdi, sifre }).then((r) => r.data),
  refresh: (refreshToken) =>
    api.post('/api/auth/refresh', { refreshToken }).then((r) => r.data),
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
  iptal: (id) => api.patch('/api/rezervasyonlar/' + id + '/iptal').then((r) => r.data),
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
};

/* ── /api/ogrenciler ───────────────────────────────────────── */
export const ogrenciService = {
  /** params: { grupId, q, odemeBekleyen, sayfa (0 tabanlı), boyut } — sayfalı yanıt { icerik, sayfa, boyut, toplamKayit, toplamSayfa } döner */
  list: (params) => api.get('/api/ogrenciler', { params }).then((r) => r.data),
  get: (id) => api.get('/api/ogrenciler/' + id).then((r) => r.data),
  create: (body) => api.post('/api/ogrenciler', body).then((r) => r.data),
  update: (id, body) => api.put('/api/ogrenciler/' + id, body).then((r) => r.data),
  remove: (id) => api.delete('/api/ogrenciler/' + id),
};

/* ── /api/ogrenciler/{id}/odemeler (öğrenci detayındaki aylık ödemeleri) ── */
export const ogrenciOdemeService = {
  /** { yil, bugun (sunucu tarihi), aylar: [{ yil, ay, odenenTutar, durum (ODENDI | ODENMEDI), odemeSekli, hesapId, hesapAd, odemeTarihi, not }] (12 ay), toplamOdenen, yillar } */
  getir: (ogrenciId, yil) => api.get('/api/ogrenciler/' + ogrenciId + '/odemeler/' + yil).then((r) => r.data),
  /** body: { aylar: [{ ay, odenenTutar, odemeSekli, hesapId, odemeTarihi }] } — yalnızca gönderilen aylar kaydedilir; tutar 0/boş ise o ayın kaydı silinir */
  kaydet: (ogrenciId, yil, body) =>
    api.put('/api/ogrenciler/' + ogrenciId + '/odemeler/' + yil, body).then((r) => r.data),
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
  /** [{ id, ad, toplamTutar, kayitTarihi, guncellemeTarihi, kaydeden, kalemler: [{ envanterId, tur, turAd, beden, adet, satisFiyati }] }] — ada göre */
  list: () => api.get('/api/envanter-setleri').then((r) => r.data),
  get: (id) => api.get('/api/envanter-setleri/' + id).then((r) => r.data),
  /** body: { ad, envanterIdleri: [id] } — her türden en fazla bir envanter kaydı (tür + beden) */
  create: (body) => api.post('/api/envanter-setleri', body).then((r) => r.data),
  update: (id, body) => api.put('/api/envanter-setleri/' + id, body).then((r) => r.data),
  remove: (id) => api.delete('/api/envanter-setleri/' + id),
};

/* ── /api/ogrenciler/{id}/envanter-satislari (öğrenci detayındaki envanter seti satışı) ── */
export const envanterSatisService = {
  list: (ogrenciId) => api.get('/api/ogrenciler/' + ogrenciId + '/envanter-satislari').then((r) => r.data),
  /** body: { kalemler: [{ tur, beden, odendi, teslim }] (her türden bir tane), odenenTutar, setId (isteğe bağlı; yalnızca oluştururken) } */
  create: (ogrenciId, body) =>
    api.post('/api/ogrenciler/' + ogrenciId + '/envanter-satislari', body).then((r) => r.data),
  update: (ogrenciId, satisId, body) =>
    api.put('/api/ogrenciler/' + ogrenciId + '/envanter-satislari/' + satisId, body).then((r) => r.data),
  remove: (ogrenciId, satisId) =>
    api.delete('/api/ogrenciler/' + ogrenciId + '/envanter-satislari/' + satisId),
};

/* ── /api/gruplar ──────────────────────────────────────────── */
export const grupService = {
  list: () => api.get('/api/gruplar').then((r) => r.data),
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
