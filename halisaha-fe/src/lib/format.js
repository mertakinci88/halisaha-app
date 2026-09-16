export const GUNLER = ['Pazar', 'Pazartesi', 'Salı', 'Çarşamba', 'Perşembe', 'Cuma', 'Cumartesi'];
export const AYLAR = [
  'Ocak', 'Şubat', 'Mart', 'Nisan', 'Mayıs', 'Haziran',
  'Temmuz', 'Ağustos', 'Eylül', 'Ekim', 'Kasım', 'Aralık',
];

export const tl = (n) =>
  '₺' + Number(n || 0).toLocaleString('tr-TR', { maximumFractionDigits: 2 });

export const hh = (h) => String(h).padStart(2, '0') + ':00';

/** Date -> "2026-09-12" (backend @DateTimeFormat ISO.DATE) */
export const toIsoDate = (date) => {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return y + '-' + m + '-' + d;
};

/** Date + saat -> "2026-09-12T19:00:00" (backend LocalDateTime) */
export const toIsoDateTime = (date, hour) =>
  toIsoDate(date) + 'T' + String(hour).padStart(2, '0') + ':00:00';

export const uzunTarih = (date) =>
  date.getDate() + ' ' + AYLAR[date.getMonth()] + ' ' + date.getFullYear() + ', ' + GUNLER[date.getDay()];

export const kisaTarih = (isoDate) => {
  if (!isoDate) return '—';
  const [y, m, d] = String(isoDate).slice(0, 10).split('-');
  return d + '.' + m + '.' + y;
};

export const saatOf = (isoDateTime) => String(isoDateTime || '').slice(11, 16) || '—';

export const saatSayisi = (isoDateTime) => {
  const hhmm = String(isoDateTime || '').slice(11, 13);
  return Number(hhmm || 0);
};

export const sureSaat = (baslangic, bitis) =>
  Math.max(1, saatSayisi(bitis) - saatSayisi(baslangic));

export const ODEME_DURUMU_LABEL = {
  ODENDI: 'Ödendi',
  ODENMEDI: 'Ödenmedi',
  KISMI_ODENDI: 'Kısmi ödendi',
};

export const ODEME_YONTEMI_LABEL = {
  NAKIT: 'Nakit',
  KART: 'Kart',
  HAVALE_EFT: 'Havale / EFT',
};
