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
};

/* ── /api/kullanicilar (yalnızca admin) ───────────────────────── */
export const kullaniciService = {
  list: () => api.get('/api/kullanicilar').then((r) => r.data),
  get: (id) => api.get('/api/kullanicilar/' + id).then((r) => r.data),
  create: (body) => api.post('/api/kullanicilar', body).then((r) => r.data),
  /** body: { adSoyad, sifre? } — sifre boş/yok ise değişmez */
  guncelle: (id, body) => api.put('/api/kullanicilar/' + id, body).then((r) => r.data),
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
  /** params: { grupId, q, sayfa (0 tabanlı), boyut } — sayfalı yanıt { icerik, sayfa, boyut, toplamKayit, toplamSayfa } döner */
  list: (params) => api.get('/api/ogrenciler', { params }).then((r) => r.data),
  get: (id) => api.get('/api/ogrenciler/' + id).then((r) => r.data),
  create: (body) => api.post('/api/ogrenciler', body).then((r) => r.data),
  update: (id, body) => api.put('/api/ogrenciler/' + id, body).then((r) => r.data),
  remove: (id) => api.delete('/api/ogrenciler/' + id),
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
