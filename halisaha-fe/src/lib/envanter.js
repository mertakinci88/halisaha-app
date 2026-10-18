/** Envanter türleri (Türkçe alfabetik sıra) ve beden listesi; backend'deki EnvanterTur / EnvanterBeden ile aynıdır. */
export const ENVANTER_TURLERI = [
  { kod: 'KALECI_FORMA', ad: 'Kaleci Forma', bedenli: true },
  { kod: 'KISLIK_ESOFMAN', ad: 'Kışlık Eşofman', bedenli: true },
  { kod: 'KRAMPON_CANTASI', ad: 'Krampon Çantası', bedenli: false },
  { kod: 'TOZLUK', ad: 'Tozluk', bedenli: false },
  { kod: 'YAGMURLUK', ad: 'Yağmurluk', bedenli: true },
  { kod: 'YAZLIK_FORMA', ad: 'Yazlık Forma', bedenli: true },
];

export const BEDENLER = ['128', '140', '152', '164', '176', 'Small', 'Medium'];

export const STANDART = 'STANDART';

/** Tozluk çift olarak sayılır (1 stok = 1 çift); diğer ürünler adet. */
export const birimAd = (turKod) => (turKod === 'TOZLUK' ? 'çift' : 'adet');

/** Listelerde miktar: tozlukta "40 çift", diğerlerinde yalnızca sayı (sütun başlığı Adet). */
export const miktar = (turKod, n) => (turKod === 'TOZLUK' ? n + ' çift' : n);

export const turBul = (kod) => ENVANTER_TURLERI.find((t) => t.kod === kod);
