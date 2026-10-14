import { useState } from 'react';
import { Seg, SegOpt } from '@/components/ui/seg';
import { GirisGorunumu, IadelerGorunumu, IptallerGorunumu, YoklamaGorunumu } from './KullaniciListeler';
import { IslemlerGorunumu, KasaGorunumu } from './KullaniciKasaIslemler';

const ALT_SEKMELER = [
  { kod: 'KASA', ad: 'Kasa' },
  { kod: 'ISLEMLER', ad: 'İşlemler' },
  { kod: 'IADELER', ad: 'İadeler' },
  { kod: 'IPTALLER', ad: 'İptaller' },
  { kod: 'YOKLAMA', ad: 'Yoklama' },
  { kod: 'GIRIS', ad: 'Giriş geçmişi' },
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
              {s.ad}
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
