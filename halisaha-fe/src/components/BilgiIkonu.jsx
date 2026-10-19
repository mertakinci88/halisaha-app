import { useEffect, useRef, useState } from 'react';
import { Info } from 'lucide-react';
import { cn } from '@/lib/utils';

/**
 * Alan başlığının yanına konan küçük (i) ikonu: üzerine gelince, odaklanınca ya da dokununca açıklama balonu açılır.
 * Dışarı dokunulunca kapanır (iOS'ta düğmeler dokunuşla odaklanmadığı için). Rapor kartlarındaki Bilgi ile aynı davranış;
 * farkı, köşeye değil satır içine yerleşmesidir. Dar ekranda balon, ikonu saran alanın (relative kutu) genişliğinde açılır; bu yüzden kullanan yerin sarmalayıcısı relative olmalıdır (Field öyledir). sag: geniş ekranda balon ikonun soluna doğru açılır (sağ kenardaki alanlar için).
 */
export default function BilgiIkonu({ metin, sag = false }) {
  const [acik, setAcik] = useState(false);
  const kapRef = useRef(null);

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
    <span ref={kapRef} className="group ml-1 inline-flex align-middle lg:relative" onBlur={() => setAcik(false)}>
      <button
        type="button"
        aria-label="Açıklamayı göster"
        aria-expanded={acik}
        onClick={(e) => {
          e.preventDefault();
          setAcik((a) => !a);
        }}
        className="flex h-5 w-5 cursor-pointer items-center justify-center border-0 bg-transparent p-0 text-ink/45 hover:text-accent-700 focus-visible:text-accent-700"
      >
        <Info size={15} strokeWidth={1.5} />
      </button>
      <span
        role="tooltip"
        className={cn(
          // Dar ekranda balon, alanı (Field / etiket) saran kutunun genişliğinde ve altında açılır (sağa taşmaz); geniş ekranda ikonun altında
          'absolute inset-x-0 top-full z-20 mt-1 border border-divider bg-bg px-3 py-2.5 text-left text-[13px] font-normal normal-case leading-snug tracking-normal text-ink shadow-md',
          'lg:inset-x-auto lg:w-[270px]',
          sag ? 'lg:right-0' : 'lg:left-0',
          acik ? 'block' : 'hidden group-hover:block group-focus-within:block',
        )}
      >
        {metin}
      </span>
    </span>
  );
}

/**
 * Seçenek listesi açıklaması için balon içeriği: isteğe bağlı giriş cümlesi ve her seçenek için ayrı satırda
 * kalın başlık + açıklama. BilgiIkonu'nun metin alanına verilir.
 * maddeler: [[başlık, açıklama], ...]
 */
export function BilgiMaddeleri({ giris, maddeler }) {
  return (
    <span className="flex flex-col gap-1.5">
      {giris ? <span className="block text-ink/70">{giris}</span> : null}
      {maddeler.map(([baslik, aciklama]) => (
        <span key={baslik} className="block">
          <b className="font-semibold">{baslik}</b>
          <span className="block text-ink/80">{aciklama}</span>
        </span>
      ))}
    </span>
  );
}
