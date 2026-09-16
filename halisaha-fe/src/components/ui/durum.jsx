import { Card } from '@/components/ui/card';

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
    <Card className="border-accent-700 gap-2">
      <div className="font-heading text-[20px] text-accent-800">Bir şeyler ters gitti</div>
      <p className="m-0 text-[15px] text-ink/75">{mesaj}</p>
      {onTekrar ? (
        <button type="button" onClick={onTekrar} className="self-start text-accent-700 underline cursor-pointer bg-transparent border-0 p-0 font-body text-[15px]">
          Tekrar dene
        </button>
      ) : null}
    </Card>
  );
}
