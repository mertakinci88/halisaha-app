import { useMemo, useState } from 'react';
import { PageHeader } from '@/components/AppLayout';
import { Seg, SegOpt } from '@/components/ui/seg';
import { Input, Field } from '@/components/ui/input';
import FinansSekmesi from '@/components/rapor/FinansSekmesi';
import OgrenciSekmesi from '@/components/rapor/OgrenciSekmesi';
import YoklamaSekmesi from '@/components/rapor/YoklamaSekmesi';
import RezervasyonSekmesi from '@/components/rapor/RezervasyonSekmesi';
import EnvanterSekmesi from '@/components/rapor/EnvanterSekmesi';
import { DONEMLER } from '@/lib/rapor';

const SEKMELER = [
  { kod: 'FINANS', ad: 'Finans' },
  { kod: 'OGRENCI', ad: 'Öğrenci' },
  { kod: 'YOKLAMA', ad: 'Yoklama' },
  { kod: 'REZERVASYON', ad: 'Rezervasyon' },
  { kod: 'ENVANTER', ad: 'Envanter' },
];

export default function RaporPage() {
  const [sekme, setSekme] = useState('FINANS');
  const [donem, setDonem] = useState('AY');
  const [ozel, setOzel] = useState(() => DONEMLER.find((d) => d.kod === 'AY').aralik());

  const aralik = useMemo(
    () => (donem === 'OZEL' ? ozel : DONEMLER.find((d) => d.kod === donem).aralik()),
    [donem, ozel],
  );
  // Tarih tamamlanmamış ya da ters girilmişse istek atılmaz
  const gecerli = Boolean(aralik.baslangic && aralik.bitis && aralik.baslangic <= aralik.bitis);
  const { baslangic, bitis } = aralik;

  return (
    <>
      <PageHeader kicker="Yönetim" baslik="Raporlar" />

      <div className="mb-5 flex flex-col gap-3">
        <div className="max-w-full overflow-x-auto">
          <Seg>
            {SEKMELER.map((s) => (
              <SegOpt className="whitespace-nowrap" key={s.kod} active={sekme === s.kod} onClick={() => setSekme(s.kod)}>
                {s.ad}
              </SegOpt>
            ))}
          </Seg>
        </div>

        <div className="flex flex-wrap items-end gap-3">
          <div className="max-w-full overflow-x-auto">
            <Seg>
              {DONEMLER.map((d) => (
                <SegOpt className="whitespace-nowrap" key={d.kod} active={donem === d.kod} onClick={() => setDonem(d.kod)}>
                  {d.ad}
                </SegOpt>
              ))}
              <SegOpt className="whitespace-nowrap" active={donem === 'OZEL'} onClick={() => setDonem('OZEL')}>
                Özel
              </SegOpt>
            </Seg>
          </div>
          {donem === 'OZEL' ? (
            <div className="flex flex-wrap gap-3">
              <Field label="Başlangıç">
                <Input type="date" value={ozel.baslangic} onChange={(e) => setOzel({ ...ozel, baslangic: e.target.value })} />
              </Field>
              <Field label="Bitiş">
                <Input type="date" value={ozel.bitis} onChange={(e) => setOzel({ ...ozel, bitis: e.target.value })} />
              </Field>
            </div>
          ) : null}
        </div>
      </div>

      {!gecerli ? (
        <p className="text-ink/60">Geçerli bir tarih aralığı seçin (başlangıç, bitişten sonra olamaz).</p>
      ) : sekme === 'FINANS' ? (
        <FinansSekmesi baslangic={baslangic} bitis={bitis} />
      ) : sekme === 'OGRENCI' ? (
        <OgrenciSekmesi baslangic={baslangic} bitis={bitis} />
      ) : sekme === 'YOKLAMA' ? (
        <YoklamaSekmesi baslangic={baslangic} bitis={bitis} />
      ) : sekme === 'REZERVASYON' ? (
        <RezervasyonSekmesi baslangic={baslangic} bitis={bitis} />
      ) : (
        <EnvanterSekmesi baslangic={baslangic} bitis={bitis} />
      )}
    </>
  );
}
