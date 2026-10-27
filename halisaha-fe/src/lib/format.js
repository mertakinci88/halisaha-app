export const GUNLER = ['Pazar', 'Pazartesi', 'Salı', 'Çarşamba', 'Perşembe', 'Cuma', 'Cumartesi'];
export const AYLAR = [
  'Ocak', 'Şubat', 'Mart', 'Nisan', 'Mayıs', 'Haziran',
  'Temmuz', 'Ağustos', 'Eylül', 'Ekim', 'Kasım', 'Aralık',
];

export const tl = (n) =>
  '₺' + Number(n || 0).toLocaleString('tr-TR', { maximumFractionDigits: 2 });

/** 24 ve üzeri saatler ertesi güne taşar (26 -> 02:00) */
/** Girdiden en fazla 10 haneli telefon rakamı çıkarır: başındaki 0 / +90 / 90 atılır (5469764613). */
export const telRakam = (deger) => {
  let r = String(deger || '').replace(/\D/g, '');
  if (r.startsWith('90') && r.length > 10) r = r.slice(2);
  if (r.startsWith('0')) r = r.slice(1);
  return r.slice(0, 10);
};

/** Kayıtlı numara (05469764613) -> "0546 976 46 13"; beklenen biçimde değilse olduğu gibi döner. */
export const telGoster = (telNo) => {
  const r = String(telNo || '');
  return /^05\d{9}$/.test(r) ? '0' + telMaske(r) : r || '—';
};

/** 5469764613 -> "546 976 46 13" */
export const telMaske = (rakam) => {
  const r = telRakam(rakam);
  return [r.slice(0, 3), r.slice(3, 6), r.slice(6, 8), r.slice(8, 10)].filter(Boolean).join(' ');
};

/** "tr33 0006…" -> "TR33 0006 1005 …" (boşluk/harf dışı atılır, en çok 26 karakter, 4'lü gruplanır) */
export const ibanMaske = (deger) =>
  String(deger || '')
    .replace(/[^a-zA-Z0-9]/g, '')
    .toUpperCase()
    .slice(0, 26)
    .replace(/(.{4})/g, '$1 ')
    .trim();

/** Saat (ondalıklı olabilir: 12.5 = 12:30) -> "12:30"; 24 ve üzeri ertesi güne taşar (25 -> "01:00"). */
export const hh = (h) => {
  const toplamDakika = Math.round(h * 60);
  return String(Math.floor(toplamDakika / 60) % 24).padStart(2, '0') + ':' + String(toplamDakika % 60).padStart(2, '0');
};

/** Date -> "2026-09-12" (backend @DateTimeFormat ISO.DATE) */
export const toIsoDate = (date) => {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return y + '-' + m + '-' + d;
};

/** Date + saat -> "2026-09-12T19:00:00" (backend LocalDateTime) */
export const toIsoDateTime = (date, hour) => {
  const gun = new Date(date);
  gun.setDate(gun.getDate() + Math.floor(hour / 24));
  const dakika = Math.round((hour % 24) * 60);
  return toIsoDate(gun) + 'T' + String(Math.floor(dakika / 60)).padStart(2, '0') + ':' + String(dakika % 60).padStart(2, '0') + ':00';
};

export const uzunTarih = (date) =>
  date.getDate() + ' ' + AYLAR[date.getMonth()] + ' ' + date.getFullYear() + ', ' + GUNLER[date.getDay()];

export const kisaTarih = (isoDate) => {
  if (!isoDate) return '—';
  const [y, m, d] = String(isoDate).slice(0, 10).split('-');
  return d + '.' + m + '.' + y;
};

/** 2026-10-11T12:30:00 -> "11.10.2026 12:30" */
export const tarihSaat = (isoDateTime) =>
  isoDateTime ? kisaTarih(isoDateTime) + ' ' + String(isoDateTime).slice(11, 16) : '—';

export const saatOf = (isoDateTime) => String(isoDateTime || '').slice(11, 16) || '—';

export const saatSayisi = (isoDateTime) => {
  const hhmm = String(isoDateTime || '').slice(11, 13);
  return Number(hhmm || 0);
};

/** "2026-10-11T12:30:00" -> 12.5 (saat + dakika/60) */
export const gunSaati = (isoDateTime) => {
  const s = String(isoDateTime || '');
  return Number(s.slice(11, 13) || 0) + Number(s.slice(14, 16) || 0) / 60;
};

/** İki zaman arasındaki süre, saat olarak (1.5 = bir buçuk saat). */
export const sureSaat = (baslangic, bitis) => Math.max(0, (new Date(bitis) - new Date(baslangic)) / 3600000);

/** 1 -> "1", 1.5 -> "1,5" */
export const saatYazi = (n) => Number(n).toLocaleString('tr-TR', { maximumFractionDigits: 2 });

export const ODEME_DURUMU_LABEL = {
  ODENDI: 'Ödendi',
  ODENMEDI: 'Ödenmedi',
  KISMI_ODENDI: 'Kısmi ödendi',
  KISMI_GECIKMIS: 'Kısmi ödendi · Gecikmiş',
  ODEME_BEKLIYOR: 'Ödeme bekliyor',
  ODEMESIZ: 'Ödemesiz',
};

/**
 * Ürün stok durumu: 0 ise TUKENDI, ürünün stok uyarı adedinden azsa AZ, değilse null.
 * (Uyarı adedi tanımsızsa eski varsayılan olan 10 kullanılır.)
 */
export const stokDurumu = (urun) => {
  if (Number(urun.adet) <= 0) return 'TUKENDI';
  return Number(urun.adet) < Number(urun.stokUyariAdedi ?? 10) ? 'AZ' : null;
};

export const ODEME_TIPI_LABEL = {
  KAPORA: 'Kapora',
  KISMI: 'Kısmi ödeme',
  ODEME: 'Ödeme',
};

/** Girilen tutarı 0 ile üst sınır arasında tutar (kapora/kısmi ödeme ödenecek tutarı aşamaz). */
export const tutarSinirla = (deger, ust) => {
  if (deger === '') return '';
  const sayi = Number(String(deger).replace(',', '.'));
  if (Number.isNaN(sayi)) return '';
  if (sayi < 0) return '0';
  return sayi > ust ? String(ust) : deger;
};

export const ODEME_YONTEMI_LABEL = {
  NAKIT: 'Nakit',
  KART: 'Kart',
  HAVALE_EFT: 'Havale / EFT',
};

export const TRANSFER_DURUM = {
  BEKLIYOR: { ad: 'Onay bekliyor', tone: 'warning' },
  ONAYLANDI: { ad: 'Onaylandı', tone: 'accent' },
  REDDEDILDI: { ad: 'Reddedildi', tone: 'danger' },
  IPTAL: { ad: 'İptal edildi', tone: 'neutral' },
};
