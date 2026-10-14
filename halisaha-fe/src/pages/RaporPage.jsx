import { createElement, useMemo, useState } from 'react';
import { Banknote, Calendar, CalendarCheck, CalendarDays, CalendarMinus, CalendarRange, CalendarSearch, ClipboardCheck, Package, UserRound, Users } from 'lucide-react';
import { PageHeader } from '@/components/AppLayout';
import { Seg, SegOpt } from '@/components/ui/seg';
import { Input, Field } from '@/components/ui/input';
import FinansSekmesi from '@/components/rapor/FinansSekmesi';
import OgrenciSekmesi from '@/components/rapor/OgrenciSekmesi';
import YoklamaSekmesi from '@/components/rapor/YoklamaSekmesi';
import RezervasyonSekmesi from '@/components/rapor/RezervasyonSekmesi';
import EnvanterSekmesi from '@/components/rapor/EnvanterSekmesi';
import KullaniciSekmesi from '@/components/rapor/KullaniciSekmesi';
import { DONEMLER } from '@/lib/rapor';

const DONEM_IKON = { BUGUN: CalendarCheck, HAFTA: CalendarRange, AY: CalendarDays, GECEN_AY: CalendarMinus, YIL: Calendar };

const SEKMELER = [
  { kod: 'FINANS', Ikon: Banknote, ad: 'Finans' },
  { kod: 'OGRENCI', Ikon: Users, ad: 'Öğrenci' },
  { kod: 'YOKLAMA', Ikon: ClipboardCheck, ad: 'Yoklama' },
  { kod: 'REZERVASYON', Ikon: CalendarDays, ad: 'Rezervasyon' },
  { kod: 'ENVANTER', Ikon: Package, ad: 'Envanter' },
  { kod: 'KULLANICI', Ikon: UserRound, ad: 'Kullanıcı' },
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
                <s.Ikon size={16} strokeWidth={1.5} />
                <span>{s.ad}</span>
              </SegOpt>
            ))}
          </Seg>
        </div>

        <div className="flex flex-wrap items-end gap-3">
          <div className="max-w-full overflow-x-auto">
            <Seg>
              {DONEMLER.map((d) => (
                <SegOpt className="whitespace-nowrap" key={d.kod} active={donem === d.kod} onClick={() => setDonem(d.kod)}>
                  {createElement(DONEM_IKON[d.kod] || Calendar, { size: 16, strokeWidth: 1.5 })}
                  <span>{d.ad}</span>
                </SegOpt>
              ))}
              <SegOpt className="whitespace-nowrap" active={donem === 'OZEL'} onClick={() => setDonem('OZEL')}>
                <CalendarSearch size={16} strokeWidth={1.5} />
                <span>Özel</span>
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
      ) : sekme === 'ENVANTER' ? (
        <EnvanterSekmesi baslangic={baslangic} bitis={bitis} />
      ) : (
        <KullaniciSekmesi baslangic={baslangic} bitis={bitis} />
      )}
    </>
  );
}
