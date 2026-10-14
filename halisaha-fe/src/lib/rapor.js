import { toIsoDate } from '@/lib/format';

/** Grafik serileri için renkler (recharts CSS değişkeni okuyamadığı için index.css token'larının karşılığı). */
export const RENK = {
  rezervasyon: '#597ea3', // accent-600
  ogrenci: '#2f8a52', // basari-600
  envanter: '#c2872b',
  gelmedi: '#c0504d',
  izinli: '#b5d9fd', // accent-300
  izgara: 'rgba(29, 31, 32, 0.12)',
  yazi: 'rgba(29, 31, 32, 0.6)',
};

export const GUN_KISA = ['Pzt', 'Sal', 'Çar', 'Per', 'Cum', 'Cmt', 'Paz'];

const gunEkle = (tarih, gun) => {
  const t = new Date(tarih);
  t.setDate(t.getDate() + gun);
  return t;
};

/** Rapor dönemi ön ayarları; her biri { baslangic, bitis } (ISO tarih) üretir. */
export const DONEMLER = [
  { kod: 'BUGUN', ad: 'Bugün', aralik: () => ({ baslangic: toIsoDate(new Date()), bitis: toIsoDate(new Date()) }) },
  {
    kod: 'HAFTA',
    ad: 'Bu hafta',
    aralik: () => {
      const bugun = new Date();
      const pazartesi = gunEkle(bugun, -((bugun.getDay() + 6) % 7));
      return { baslangic: toIsoDate(pazartesi), bitis: toIsoDate(bugun) };
    },
  },
  {
    kod: 'AY',
    ad: 'Bu ay',
    aralik: () => {
      const bugun = new Date();
      return { baslangic: toIsoDate(new Date(bugun.getFullYear(), bugun.getMonth(), 1)), bitis: toIsoDate(bugun) };
    },
  },
  {
    kod: 'GECEN_AY',
    ad: 'Geçen ay',
    aralik: () => {
      const bugun = new Date();
      return {
        baslangic: toIsoDate(new Date(bugun.getFullYear(), bugun.getMonth() - 1, 1)),
        bitis: toIsoDate(new Date(bugun.getFullYear(), bugun.getMonth(), 0)),
      };
    },
  },
  {
    kod: 'YIL',
    ad: 'Bu yıl',
    aralik: () => {
      const bugun = new Date();
      return { baslangic: toIsoDate(new Date(bugun.getFullYear(), 0, 1)), bitis: toIsoDate(bugun) };
    },
  },
];

/** Önceki dönemle yüzde fark; önceki dönem 0 ise karşılaştırma anlamsız olduğundan null. */
export const yuzdeFark = (simdi, onceki) => {
  const o = Number(onceki);
  if (!o) return null;
  return ((Number(simdi) - o) / o) * 100;
};

export const yuzde = (n) => '%' + Number(n || 0).toLocaleString('tr-TR', { maximumFractionDigits: 1 });

/** Eksen için kısa tutar: 12.500 -> "12,5B" yerine sade "12,5 bin". */
export const tlKisa = (n) => {
  const v = Number(n || 0);
  if (Math.abs(v) >= 1000) return (v / 1000).toLocaleString('tr-TR', { maximumFractionDigits: 1 }) + ' bin';
  return String(v);
};
