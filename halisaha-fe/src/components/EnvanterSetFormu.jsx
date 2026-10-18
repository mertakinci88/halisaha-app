import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Check, Save, TriangleAlert, X } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Field, Input, Select } from '@/components/ui/input';
import { Tag } from '@/components/ui/tag';
import { Yukleniyor } from '@/components/ui/durum';
import { envanterService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata } from '@/context/ToastContext';
import { tl } from '@/lib/format';
import { BEDENLER, ENVANTER_TURLERI, birimAd } from '@/lib/envanter';
import { cn } from '@/lib/utils';

/**
 * Yeni set ve set güncelleme formu. Beden seçilince o bedendeki bedenli envanterler (kaleci forma, eşofman, yağmurluk,
 * yazlık forma) ve bedensiz ürünler (kampon çantası, tozluk) listelenir; sete girecekleri kullanıcı işaretler.
 * Hiçbir ürün hazır işaretli gelmez. Envanter kaydı olmayan ya da stoğu biten ürün işaretlenemez.
 */
export default function EnvanterSetFormu({ baslangic, kaydetEtiketi, kaydediyor, onKaydet }) {
  const navigate = useNavigate();
  const [, setHata] = useHata();
  const [envanterler, setEnvanterler] = useState(null);
  const [ad, setAd] = useState(baslangic?.ad || '');
  const [beden, setBeden] = useState(baslangic?.beden || '');
  const [tutar, setTutar] = useState(baslangic?.tutar != null ? String(Number(baslangic.tutar)) : '');
  const [secili, setSecili] = useState(() => new Set(baslangic?.envanterIdleri || []));

  useEffect(() => {
    envanterService
      .list()
      .then(setEnvanterler)
      .catch((e) => setHata(apiHata(e), { sessiz: true }));
  }, []);

  if (!envanterler) return <Yukleniyor />;

  // Seçilen bedenin satırları: bedenli türler (set bedeninde) ve bedensiz türler (varsa)
  const satirlar = beden
    ? ENVANTER_TURLERI.map((tur) => ({
        tur,
        env: envanterler.find((e) => e.tur === tur.kod && e.beden === (tur.bedenli ? beden : 'STANDART')),
      })).filter((r) => r.env || r.tur.bedenli)
    : [];
  const durumu = (r) => (!r.env ? 'YOK' : r.env.adet <= 0 ? 'STOKSUZ' : 'TAMAM');
  const seciliSatirlar = satirlar.filter((r) => r.env && secili.has(r.env.id));
  const stoksuzSecili = seciliSatirlar.filter((r) => durumu(r) === 'STOKSUZ');
  const tutarSayi = Number(tutar);
  const kaydetPasif =
    kaydediyor || !ad.trim() || !beden || !(tutarSayi > 0) || seciliSatirlar.length === 0 || stoksuzSecili.length > 0;

  const bedenDegisti = (yeniBeden) => {
    setBeden(yeniBeden);
    // Hiçbir ürün hazır işaretli gelmez; önceki bedenin bedenli seçimleri temizlenir, bedensiz seçimler korunur
    setSecili((onceki) => new Set(envanterler.filter((e) => onceki.has(e.id) && e.beden === 'STANDART').map((e) => e.id)));
  };

  const sec = (id) =>
    setSecili((onceki) => {
      const yeni = new Set(onceki);
      if (yeni.has(id)) yeni.delete(id);
      else yeni.add(id);
      return yeni;
    });

  const gonder = (ev) => {
    ev.preventDefault();
    onKaydet({ ad: ad.trim(), beden, tutar: tutarSayi, envanterIdleri: seciliSatirlar.map((r) => r.env.id) });
  };

  return (
    <form onSubmit={gonder} className="max-w-[640px]">
      <Card className="gap-3.5">
        <div className="flex flex-wrap items-end gap-3">
          <h4 className="text-[22px]">Set bilgileri</h4>
          <div className="ml-auto text-right leading-none">
            <div className="text-[11.5px] uppercase tracking-[0.12em] text-ink/55">Set tutarı</div>
            <div className="font-heading text-[34px] font-semibold text-accent-700">{tl(tutarSayi > 0 ? tutarSayi : 0)}</div>
          </div>
        </div>

        <Field label="Set adı">
          <Input
            value={ad}
            onChange={(e) => setAd(e.target.value)}
            maxLength={100}
            placeholder="Örn. Yazlık Set 140"
            required
            className="text-[16px]"
          />
        </Field>

        <div className="grid grid-cols-1 gap-3 min-[480px]:grid-cols-2">
          <Field label="Beden">
            <Select value={beden} onChange={(e) => bedenDegisti(e.target.value)} required className="text-[16px]">
              <option value="">Seçiniz</option>
              {BEDENLER.map((b) => (
                <option key={b} value={b}>{b}</option>
              ))}
            </Select>
          </Field>
          <Field label="Set tutarı (₺)">
            <Input
              type="number"
              min="1"
              step="1"
              inputMode="decimal"
              value={tutar}
              onChange={(e) => setTutar(e.target.value)}
              placeholder="0"
              required
              className="text-[16px]"
            />
          </Field>
        </div>

        <div>
          <div className="mb-1 text-[13.5px] text-ink/70">Sette yer alacak envanterler</div>
          {!beden ? (
            <div className="border border-dashed border-ink/30 px-4 py-6 text-[14.5px] text-ink/60">
              Beden seçince o bedendeki envanterler burada listelenir; sete girecekleri siz işaretlersiniz.
            </div>
          ) : (
            <div className="flex flex-col border border-divider">
              {satirlar.map((r) => {
                const durum = durumu(r);
                const on = Boolean(r.env && secili.has(r.env.id));
                const pasif = durum === 'YOK' || (durum === 'STOKSUZ' && !on);
                return (
                  <label
                    key={r.tur.kod}
                    className={cn(
                      'relative flex min-h-11 flex-wrap items-center gap-x-3 border-b border-hairline px-3 py-2 last:border-b-0',
                      pasif ? 'cursor-not-allowed opacity-60' : 'cursor-pointer',
                      on && 'bg-accent-100',
                    )}
                  >
                    <input
                      type="checkbox"
                      className="peer sr-only"
                      checked={on}
                      disabled={pasif}
                      onChange={() => sec(r.env.id)}
                      aria-label={r.tur.ad}
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
                    <span className="font-semibold">{r.tur.ad}</span>
                    <span className="text-ink/60">{r.tur.bedenli ? beden : 'Standart'}</span>
                    <span className="ml-auto">
                      {durum === 'YOK' ? (
                        <Tag tone="danger">Envanter kaydı yok</Tag>
                      ) : durum === 'STOKSUZ' ? (
                        <Tag tone="danger">Stok yok</Tag>
                      ) : (
                        <span className="text-[14px] tabular-nums text-ink/70">{r.env.adet} {birimAd(r.tur.kod)}</span>
                      )}
                    </span>
                  </label>
                );
              })}
            </div>
          )}
          {stoksuzSecili.length > 0 ? (
            <div role="alert" className="mt-2 flex items-start gap-2 border border-hata-600 bg-hata-100 px-3 py-2 text-[14px] text-hata-800">
              <TriangleAlert size={16} className="mt-0.5 shrink-0" />
              <span>Stoğu biten ürün sete eklenemez: {stoksuzSecili.map((r) => r.tur.ad).join(', ')}. İşaretini kaldırın.</span>
            </div>
          ) : null}
        </div>

        <div className="mt-1 flex gap-2">
          <Button type="submit" variant="primary" className="flex-1" disabled={kaydetPasif}>
            <Save size={16} strokeWidth={1.5} />
            <span>{kaydediyor ? 'Kaydediliyor…' : kaydetEtiketi}</span>
          </Button>
          <Button variant="cerceveHata" onClick={() => navigate('/envanter?sekme=setler')}>
            <X size={16} strokeWidth={1.5} />
            <span>Vazgeç</span>
          </Button>
        </div>
      </Card>
    </form>
  );
}
