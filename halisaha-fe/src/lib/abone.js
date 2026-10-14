import { toIsoDate } from '@/lib/format';

/** Haftanın günleri: no = ISO (1 = Pazartesi ... 7 = Pazar), backend ile aynıdır. */
export const HAFTA_GUNLERI = [
  { no: 1, ad: 'Pazartesi' },
  { no: 2, ad: 'Salı' },
  { no: 3, ad: 'Çarşamba' },
  { no: 4, ad: 'Perşembe' },
  { no: 5, ad: 'Cuma' },
  { no: 6, ad: 'Cumartesi' },
  { no: 7, ad: 'Pazar' },
];

export const gunAdi = (no) => HAFTA_GUNLERI.find((g) => g.no === Number(no))?.ad || '—';

/** İşletme saatleri: 12:00'da açılır, 02:00'de kapanır; başlangıç saati 12-25 (24 = 00:00, 25 = 01:00). */
export const ACILIS = 12;
export const KAPANIS = 26;
export const EN_UZUN_SURE = 4;

/** "2026-10-14" -> haftanın günü (1 = Pazartesi ... 7 = Pazar) */
export const haftaGunuOf = (isoDate) => {
  const d = new Date(isoDate + 'T00:00:00').getDay();
  return d === 0 ? 7 : d;
};

/** Başlangıçtan 1 yıl sonrası (29 Şubat'ta ayın son gününe düşer); backend ile aynı kural. */
export const birYilSonra = (isoDate) => {
  if (!isoDate) return '';
  const [y, m, d] = isoDate.split('-').map(Number);
  const sonGun = new Date(y + 1, m, 0).getDate();
  return toIsoDate(new Date(y + 1, m - 1, Math.min(d, sonGun)));
};

export const saatAraligi = (baslangicSaat, sureSaat) => {
  const hh = (h) => String(h % 24).padStart(2, '0') + ':00';
  return hh(baslangicSaat) + ' – ' + hh(baslangicSaat + sureSaat);
};
