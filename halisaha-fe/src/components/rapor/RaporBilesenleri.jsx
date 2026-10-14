import { useEffect, useRef, useState } from 'react';
import { Info } from 'lucide-react';
import { ResponsiveContainer } from 'recharts';
import { Card, CardKicker } from '@/components/ui/card';
import { Yukleniyor, Hata } from '@/components/ui/durum';
import { apiHata } from '@/lib/api';
import { cn } from '@/lib/utils';
import { yuzde as yuzdeYaz, yuzdeFark } from '@/lib/rapor';

/** Rapor verisini yükler; parametre değişince yeniden çeker. */
export function useRapor(getir, bagimlilik) {
  const [veri, setVeri] = useState(null);
  const [yukleniyor, setYukleniyor] = useState(true);
  const [hata, setHata] = useState('');
  const [deneme, setDeneme] = useState(0);

  useEffect(() => {
    let iptal = false;
    setYukleniyor(true);
    setHata('');
    getir()
      .then((v) => !iptal && setVeri(v))
      .catch((e) => !iptal && setHata(apiHata(e)))
      .finally(() => !iptal && setYukleniyor(false));
    return () => {
      iptal = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...bagimlilik, deneme]);

  return { veri, yukleniyor, hata, tekrar: () => setDeneme((d) => d + 1) };
}

/** Yükleme / hata durumunu gösterir, veri hazırsa çocuğu render eder. */
export function RaporDurumu({ rapor, children }) {
  if (rapor.hata) return <Hata mesaj={rapor.hata} onTekrar={rapor.tekrar} />;
  if (!rapor.veri) return <Yukleniyor />;
  return <div className={cn('flex flex-col gap-5 transition-opacity', rapor.yukleniyor && 'opacity-50')}>{children(rapor.veri)}</div>;
}

/**
 * Kartın sağ üst köşesindeki (i) ikonu: üzerine gelince, odaklanınca ya da dokununca açıklama balonu açılır.
 * Kartın (blueprint, relative) içine yerleştirilir.
 */
export function Bilgi({ metin }) {
  const [acik, setAcik] = useState(false);
  const kapRef = useRef(null);

  // iOS'ta düğmeler dokunuşla odaklanmadığı için dışarı dokunulunca da kapanır
  useEffect(() => {
    if (!acik) return undefined;
    const disari = (e) => {
      if (kapRef.current && !kapRef.current.contains(e.target)) setAcik(false);
    };
    document.addEventListener('pointerdown', disari);
    return () => document.removeEventListener('pointerdown', disari);
  }, [acik]);

  if (!metin) return null;
  return (
    <span ref={kapRef} className="group absolute right-3 top-3 z-10" onBlur={() => setAcik(false)}>
      <button
        type="button"
        aria-label="Açıklamayı göster"
        aria-expanded={acik}
        onClick={() => setAcik((a) => !a)}
        className="flex h-6 w-6 cursor-pointer items-center justify-center border-0 bg-transparent p-0 text-ink/45 hover:text-accent-700 focus-visible:text-accent-700"
      >
        <Info size={16} strokeWidth={1.5} />
      </button>
      <span
        role="tooltip"
        className={cn(
          'absolute right-0 top-full mt-1 w-[260px] max-w-[calc(100vw-56px)] border border-divider bg-bg px-3 py-2.5 text-left text-[13px] font-normal normal-case leading-snug tracking-normal text-ink shadow-md',
          acik ? 'block' : 'hidden group-hover:block group-focus-within:block',
        )}
      >
        {metin}
      </span>
    </span>
  );
}

/** Tek bir göstergeyi büyük rakamla gösteren özet kartı. fark: önceki döneme göre yüzde değişim (opsiyonel). */
export function Ozet({ etiket, deger, alt, vurgu = false, fark, bilgi }) {
  return (
    <Card className={cn('gap-1.5', vurgu && 'border-accent')}>
      <Bilgi metin={bilgi} />
      <CardKicker className="pr-8">{etiket}</CardKicker>
      <div className="font-heading text-[32px] leading-none">{deger}</div>
      {fark !== undefined && fark !== null ? (
        <div className={cn('text-[13px]', fark >= 0 ? 'text-basari-600' : 'text-hata-600')}>
          {fark >= 0 ? '▲' : '▼'} {yuzdeYaz(Math.abs(fark))} önceki döneme göre
        </div>
      ) : null}
      {alt ? <div className="text-[13px] text-ink/55">{alt}</div> : null}
    </Card>
  );
}

export function OzetIzgara({ children, className }) {
  return <div className={cn('grid grid-cols-1 gap-4 min-[480px]:grid-cols-2 lg:grid-cols-4', className)}>{children}</div>;
}

/** Başlıklı grafik/tablo kutusu. */
export function Kutu({ baslik, aciklama, bilgi, children, className }) {
  return (
    <Card className={className}>
      <Bilgi metin={bilgi} />
      <div className={bilgi ? 'pr-8' : undefined}>
        <h4 className="font-heading text-[22px]">{baslik}</h4>
        {aciklama ? <p className="m-0 mt-0.5 text-[13px] text-ink/55">{aciklama}</p> : null}
      </div>
      {children}
    </Card>
  );
}

/** Recharts grafiği için sabit yükseklikli kap. */
export function GrafikAlani({ yukseklik = 260, children }) {
  return (
    <div style={{ height: yukseklik }} className="w-full">
      <ResponsiveContainer width="100%" height="100%">
        {children}
      </ResponsiveContainer>
    </div>
  );
}

/** Etiket + değer + yatay oran çubuğu (0-100). */
export function OranSatiri({ etiket, deger, oran, renk }) {
  return (
    <div className="flex flex-col gap-1">
      <div className="flex items-baseline justify-between gap-3 text-[14.5px]">
        <span className="min-w-0 truncate">{etiket}</span>
        <span className="shrink-0 text-ink/70">{deger}</span>
      </div>
      <div className="h-2 w-full overflow-hidden bg-ink/10" role="progressbar" aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(oran)}>
        <div className="h-full transition-[width] duration-500" style={{ width: Math.min(100, Math.max(0, oran)) + '%', background: renk || '#597ea3' }} />
      </div>
    </div>
  );
}

export function Aciklama({ children }) {
  return <p className="m-0 text-[13px] text-ink/55">{children}</p>;
}

export { yuzdeFark };

/** Recharts tooltip içeriği: başlık + satırlar [{ ad, deger, renk }]. */
export function TooltipKutusu({ baslik, satirlar }) {
  return (
    <div className="border border-divider bg-bg px-3 py-2 text-[13.5px] shadow-sm">
      <div className="mb-1 font-heading text-[15px]">{baslik}</div>
      {satirlar.map((s) => (
        <div key={s.ad} className="flex items-center gap-2">
          {s.renk ? <i className="inline-block h-2.5 w-2.5" style={{ background: s.renk }} /> : null}
          <span className="text-ink/70">{s.ad}</span>
          <span className="ml-auto pl-3">{s.deger}</span>
        </div>
      ))}
    </div>
  );
}
