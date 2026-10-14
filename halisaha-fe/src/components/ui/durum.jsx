import { Card } from '@/components/ui/card';
import { RotateCw } from 'lucide-react';

export function Yukleniyor({ metin = 'Yükleniyor…' }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-16">
      <span className="hs-yukleniyor" aria-hidden="true">
        <i className="corner tl" />
        <i className="corner tr" />
        <i className="corner bl" />
        <i className="corner br" />
      </span>
      <span className="text-[12.5px] uppercase tracking-[0.12em] text-ink/50">{metin}</span>
    </div>
  );
}

export function Hata({ mesaj, onTekrar }) {
  if (!mesaj) return null;
  return (
    <Card className="mb-5 gap-2 border-hata-600 bg-hata-100">
      <div className="font-heading text-[20px] text-hata-800">Bir şeyler ters gitti</div>
      <p className="m-0 text-[15px] text-hata-800">{mesaj}</p>
      {onTekrar ? (
        <button type="button" onClick={onTekrar} className="inline-flex items-center gap-1.5 self-start text-hata-800 underline cursor-pointer bg-transparent border-0 p-0 font-body text-[15px]">
          <RotateCw size={15} strokeWidth={1.5} />
          <span>Tekrar dene</span>
        </button>
      ) : null}
    </Card>
  );
}
