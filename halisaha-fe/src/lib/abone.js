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

/** İşletme saatleri: 12:00'da açılır, 02:00'de kapanır. Başlangıç, gün başından itibaren dakika olarak tutulur (12:00 = 720, 20:30 = 1230). */
export const ACILIS_DAKIKA = 12 * 60;
export const KAPANIS_DAKIKA = 26 * 60;
export const EN_UZUN_SURE_SAAT = 4;

/** Seçilebilir başlangıçlar: 12:00, 12:30, ... 01:00 (dakika olarak); süre tam saat olduğundan en geç kapanışa 1 saat kala başlanır */
export const BASLANGIC_SECENEKLERI = Array.from({ length: (KAPANIS_DAKIKA - 60 - ACILIS_DAKIKA) / 30 + 1 }, (_, i) => ACILIS_DAKIKA + i * 30);

/** "dakika" (720 = 12:00, 1230 = 20:30, 1440 = 00:00) -> "20:30" */
export const dakikaSaat = (dakika) =>
  String(Math.floor(dakika / 60) % 24).padStart(2, '0') + ':' + String(dakika % 60).padStart(2, '0');

/** "20:30 – 21:30" */
export const saatAraligi = (baslangicDakika, sureDakika) => dakikaSaat(baslangicDakika) + ' – ' + dakikaSaat(baslangicDakika + sureDakika);

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

