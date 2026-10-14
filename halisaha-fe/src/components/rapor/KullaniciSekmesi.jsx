import { useState } from 'react';
import { Ban, ClipboardCheck, ListChecks, LogIn, Undo2, Wallet } from 'lucide-react';
import { Seg, SegOpt } from '@/components/ui/seg';
import { GirisGorunumu, IadelerGorunumu, IptallerGorunumu, YoklamaGorunumu } from './KullaniciListeler';
import { IslemlerGorunumu, KasaGorunumu } from './KullaniciKasaIslemler';

const ALT_SEKMELER = [
  { kod: 'KASA', Ikon: Wallet, ad: 'Kasa' },
  { kod: 'ISLEMLER', Ikon: ListChecks, ad: 'İşlemler' },
  { kod: 'IADELER', Ikon: Undo2, ad: 'İadeler' },
  { kod: 'IPTALLER', Ikon: Ban, ad: 'İptaller' },
  { kod: 'YOKLAMA', Ikon: ClipboardCheck, ad: 'Yoklama' },
  { kod: 'GIRIS', Ikon: LogIn, ad: 'Giriş geçmişi' },
];

/** Kullanıcı bazlı raporlar: kasa, işlemler, iadeler, iptaller, yoklama alma, giriş geçmişi. */
export default function KullaniciSekmesi({ baslangic, bitis }) {
  const [alt, setAlt] = useState('KASA');
  return (
    <div className="flex flex-col gap-5">
      <div className="max-w-full overflow-x-auto">
        <Seg>
          {ALT_SEKMELER.map((s) => (
            <SegOpt key={s.kod} className="whitespace-nowrap" active={alt === s.kod} onClick={() => setAlt(s.kod)}>
              <s.Ikon size={16} strokeWidth={1.5} />
              <span>{s.ad}</span>
            </SegOpt>
          ))}
        </Seg>
      </div>
      {alt === 'KASA' ? <KasaGorunumu baslangic={baslangic} bitis={bitis} /> : null}
      {alt === 'ISLEMLER' ? <IslemlerGorunumu baslangic={baslangic} bitis={bitis} /> : null}
      {alt === 'IADELER' ? <IadelerGorunumu baslangic={baslangic} bitis={bitis} /> : null}
      {alt === 'IPTALLER' ? <IptallerGorunumu baslangic={baslangic} bitis={bitis} /> : null}
      {alt === 'YOKLAMA' ? <YoklamaGorunumu baslangic={baslangic} bitis={bitis} /> : null}
      {alt === 'GIRIS' ? <GirisGorunumu baslangic={baslangic} bitis={bitis} /> : null}
    </div>
  );
}
