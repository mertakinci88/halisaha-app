import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Check } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Field, Input } from '@/components/ui/input';
import { Yukleniyor } from '@/components/ui/durum';
import { envanterService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata } from '@/context/ToastContext';
import { tl } from '@/lib/format';
import { ENVANTER_TURLERI } from '@/lib/envanter';
import { cn } from '@/lib/utils';

/**
 * Yeni set ve set güncelleme formu. Set, kayıtlı envanterlerden (tür + beden) oluşur; her türden en fazla bir
 * kayıt seçilebilir (aynı türden başka bedene tıklamak seçimi o bedene taşır). Seçilenlerin toplamı anlık gösterilir.
 */
export default function EnvanterSetFormu({ baslangic, kaydetEtiketi, kaydediyor, onKaydet }) {
  const navigate = useNavigate();
  const [, setHata] = useHata();
  const [envanterler, setEnvanterler] = useState(null);
  const [ad, setAd] = useState(baslangic?.ad || '');
  const [secili, setSecili] = useState(() => new Set(baslangic?.envanterIdleri || []));

  useEffect(() => {
    envanterService
      .list()
      .then(setEnvanterler)
      .catch((e) => setHata(apiHata(e), { sessiz: true }));
  }, []);

  if (!envanterler) return <Yukleniyor />;

  const turlu = ENVANTER_TURLERI.map((t) => ({ tur: t, kayitlar: envanterler.filter((e) => e.tur === t.kod) })).filter(
    (g) => g.kayitlar.length > 0,
  );
  const seciliKayitlar = envanterler.filter((e) => secili.has(e.id));
  const toplam = seciliKayitlar.reduce((t, e) => t + Number(e.satisFiyati), 0);

  const sec = (e) => {
    setSecili((onceki) => {
      const yeni = new Set(onceki);
      if (yeni.has(e.id)) {
        yeni.delete(e.id);
      } else {
        envanterler.filter((x) => x.tur === e.tur).forEach((x) => yeni.delete(x.id));
        yeni.add(e.id);
      }
      return yeni;
    });
  };

  const gonder = (ev) => {
    ev.preventDefault();
    onKaydet({ ad: ad.trim(), envanterIdleri: seciliKayitlar.map((e) => e.id) });
  };

  return (
    <form onSubmit={gonder} className="max-w-[640px]">
      <Card className="gap-3.5">
        <div className="flex flex-wrap items-end gap-3">
          <h4 className="text-[22px]">Set bilgileri</h4>
          <div className="ml-auto text-right leading-none">
            <div className="text-[11.5px] uppercase tracking-[0.12em] text-ink/55">Set toplam tutarı</div>
            <div className="font-heading text-[34px] font-semibold text-accent-700">{tl(toplam)}</div>
          </div>
        </div>

        <Field label="Set adı">
          <Input
            value={ad}
            onChange={(e) => setAd(e.target.value)}
            maxLength={100}
            placeholder="Örn. 128 Beden Seti"
            required
            className="text-[16px]"
          />
        </Field>

        <div>
          <div className="mb-1 text-[13.5px] text-ink/70">Sette yer alacak envanterler</div>
          {turlu.length === 0 ? (
            <div className="border border-dashed border-ink/30 px-4 py-6 text-[14.5px] text-ink/60">
              Henüz tanımlı envanter yok. Önce Envanterler sekmesinden envanter ekleyin.
            </div>
          ) : (
            <div className="flex flex-col">
              {turlu.map(({ tur, kayitlar }) => (
                <div key={tur.kod} className="border-b border-hairline py-2.5">
                  <div className="mb-1.5 font-semibold">{tur.ad}</div>
                  <div className="flex flex-wrap gap-2">
                    {kayitlar.map((e) => {
                      const on = secili.has(e.id);
                      return (
                        <label
                          key={e.id}
                          className={cn(
                            'relative inline-flex min-h-10 cursor-pointer items-center gap-2 border px-3 text-[15px]',
                            on ? 'border-accent bg-accent-100' : 'border-divider bg-bg',
                          )}
                        >
                          <input
                            type="checkbox"
                            className="peer sr-only"
                            checked={on}
                            onChange={() => sec(e)}
                            aria-label={tur.ad + (tur.bedenli ? ' ' + e.beden : '')}
                          />
                          <span
                            className={cn(
                              'grid size-[18px] place-items-center border-[1.5px] border-ink/50 bg-bg text-white',
                              'peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-accent',
                              on && 'border-accent bg-accent',
                            )}
                          >
                            {on ? <Check size={13} strokeWidth={3} /> : null}
                          </span>
                          <span>
                            {tur.bedenli ? e.beden : 'Standart'} · <span className="tabular-nums">{tl(e.satisFiyati)}</span>
                          </span>
                        </label>
                      );
                    })}
                  </div>
                </div>
              ))}
            </div>
          )}
          <p className="m-0 mt-2 text-[13px] text-ink/55">
            Her türden en fazla bir kayıt seçilebilir. Öğrenciye satış sırasında bedenler değiştirilebilir.
          </p>
        </div>

        <div className="mt-1 flex gap-2">
          <Button type="submit" variant="primary" className="flex-1" disabled={kaydediyor || seciliKayitlar.length === 0 || !ad.trim()}>
            {kaydediyor ? 'Kaydediliyor…' : kaydetEtiketi}
          </Button>
          <Button variant="secondary" onClick={() => navigate('/envanter?sekme=setler')}>
            Vazgeç
          </Button>
        </div>
      </Card>
    </form>
  );
}
