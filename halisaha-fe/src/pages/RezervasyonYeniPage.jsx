import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Field, Input, Select } from '@/components/ui/input';
import { Seg, SegOpt } from '@/components/ui/seg';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import { rezervasyonService, sahaService } from '@/services';
import { apiHata } from '@/lib/api';
import { hh, telMaske, telRakam, tl, toIsoDate, toIsoDateTime, tutarSinirla, uzunTarih } from '@/lib/format';

const ACILIS = 12;
const KAPANIS = 26; // 26 = ertesi gün 02:00
// Saatler gerçek saat olarak tutulur: 12..23, sonra 00, 01, 02 (ertesi gün)
const SAATLER = Array.from({ length: KAPANIS - ACILIS + 1 }, (_, i) => (ACILIS + i) % 24);
const gece = (saat) => saat < ACILIS;

// Havale / EFT'de gösterilen hesaplar. Şimdilik sabit; ileride bir kaynaktan (ayar/API) alınacak.
const BANKA_HESAPLARI = ['Garanti Bankası X Şubesi', 'İş Bankası Y Şubesi', 'Farklı Hesap'];

const gunEkle = (isoTarih, fark) => {
  const d = new Date(isoTarih + 'T00:00:00');
  d.setDate(d.getDate() + fark);
  return toIsoDate(d);
};

// Takvimden gelen saat 24 ve üzeriyse ertesi güne taşar (örn. 25 -> ertesi gün 01:00)
const baslangicOku = (params) => {
  const tarih = params.get('tarih') || toIsoDate(new Date());
  const saat = Number(params.get('saat') || 19);
  return saat >= 24 ? { tarih: gunEkle(tarih, 1), saat: saat - 24 } : { tarih, saat };
};

export default function RezervasyonYeniPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();

  const [sahalar, setSahalar] = useState([]);
  const [yukleniyor, setYukleniyor] = useState(true);
  const [hata, setHata] = useState('');
  const [kaydediyor, setKaydediyor] = useState(false);

  const [form, setForm] = useState(() => ({
    sahaId: params.get('sahaId') || '',
    ...baslangicOku(params),
    sure: 1,
    adSoyad: '',
    telNo: '',
    odemeYontemi: 'NAKIT',
    kapora: '',
    banka: BANKA_HESAPLARI[0],
  }));

  const set = (k, v) => setForm((p) => ({ ...p, [k]: v }));

  // Gece 00:00'dan sonrası ertesi güne ait: saat gece dilimine geçince tarih +1, dönünce -1
  const saatSec = (yeni) =>
    setForm((p) => {
      const fark = gece(yeni) === gece(p.saat) ? 0 : gece(yeni) ? 1 : -1;
      return { ...p, saat: yeni, tarih: fark ? gunEkle(p.tarih, fark) : p.tarih };
    });

  useEffect(() => {
    sahaService
      .list()
      .then((s) => {
        const aktif = s.filter((x) => x.durum === 'AKTIF');
        setSahalar(aktif);
        setForm((p) => ({ ...p, sahaId: p.sahaId || (aktif[0] ? String(aktif[0].id) : '') }));
      })
      .catch((e) => setHata(apiHata(e)))
      .finally(() => setYukleniyor(false));
  }, []);

  const secilenSaha = useMemo(
    () => sahalar.find((s) => String(s.id) === String(form.sahaId)),
    [sahalar, form.sahaId],
  );
  const toplam = Number(secilenSaha?.saatlikUcret || 0) * form.sure;
  const kapora = Number(String(form.kapora).replace(',', '.')) || 0;

  // Kapora ödenecek tutarı aşamaz: yazarken sınırla, süre/saha değişip tutar düşerse de düşür
  useEffect(() => {
    setForm((p) => {
      const sinirli = tutarSinirla(p.kapora, toplam);
      return sinirli === p.kapora ? p : { ...p, kapora: sinirli };
    });
  }, [toplam]);

  const kaydet = async (e) => {
    e.preventDefault();
    setHata('');
    if (kapora < 0 || kapora > toplam) {
      setHata('Kapora 0 ile ödenecek tutar (' + tl(toplam) + ') arasında olmalıdır.');
      return;
    }
    if (!/^5\d{9}$/.test(form.telNo)) {
      setHata('Telefon numarası 10 haneli olmalı ve 5 ile başlamalıdır (örn. 546 976 46 13).');
      return;
    }
    setKaydediyor(true);
    try {
      const gun = new Date(form.tarih + 'T00:00:00');
      const body = {
        sahaId: Number(form.sahaId),
        adSoyad: form.adSoyad.trim(),
        baslangicTarih: toIsoDateTime(gun, form.saat),
        bitisTarih: toIsoDateTime(gun, form.saat + form.sure),
        telNo: '0' + form.telNo,
        odemeYontemi: kapora > 0 ? form.odemeYontemi : null,
        kapora: kapora > 0 ? kapora : null,
      };
      const olusan = await rezervasyonService.create(body);
      navigate('/rezervasyon/' + olusan.id, { replace: true });
    } catch (err) {
      setHata(apiHata(err));
    } finally {
      setKaydediyor(false);
    }
  };

  if (yukleniyor) return <Yukleniyor />;

  return (
    <div>
      <PageHeader kicker="Rezervasyon" baslik="Yeni rezervasyon" />
      <Hata mesaj={hata} />

      <form
        onSubmit={kaydet}
        className="grid items-start gap-5 [grid-template-columns:repeat(auto-fit,minmax(300px,1fr))]"
      >
        <Card className="gap-4">
          <h4 className="text-[22px]">Rezervasyon bilgileri</h4>

          <Field label="Saha">
            <Select value={form.sahaId} onChange={(e) => set('sahaId', e.target.value)} required>
              {sahalar.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.ad} — {tl(s.saatlikUcret)}/saat
                </option>
              ))}
            </Select>
          </Field>

          <div className="grid grid-cols-2 gap-3">
            <Field label="Tarih">
              <Input type="date" value={form.tarih} onChange={(e) => set('tarih', e.target.value)} required />
            </Field>
            <Field label="Başlangıç saati">
              <Select value={form.saat} onChange={(e) => saatSec(Number(e.target.value))}>
                {SAATLER.map((h) => (
                  <option key={h} value={h}>
                    {hh(h)}
                  </option>
                ))}
              </Select>
            </Field>
          </div>

          <Field label="Süre">
            <Seg className="w-full">
              {[1, 2, 3].map((s) => (
                <SegOpt key={s} active={form.sure === s} onClick={() => set('sure', s)} className="flex-1">
                  {s} saat
                </SegOpt>
              ))}
            </Seg>
          </Field>

          <div className="grid grid-cols-2 gap-3">
            <Field label="Ad soyad">
              <Input
                type="text"
                placeholder="Örn. Mehmet Kaya"
                value={form.adSoyad}
                onChange={(e) => set('adSoyad', e.target.value)}
                required
              />
            </Field>
            <Field label="Telefon">
              <div className="flex">
                <span className="grid min-h-10 place-items-center border border-r-0 border-divider bg-surface px-3 text-[15.5px] text-ink/60">
                  0
                </span>
                <Input
                  type="tel"
                  inputMode="numeric"
                  autoComplete="tel-national"
                  placeholder="5__ ___ __ __"
                  value={telMaske(form.telNo)}
                  onChange={(e) => set('telNo', telRakam(e.target.value))}
                  required
                />
              </div>
            </Field>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Field label="Kapora ödeme yöntemi">
              <Select value={form.odemeYontemi} onChange={(e) => set('odemeYontemi', e.target.value)}>
                <option value="NAKIT">Nakit</option>
                <option value="KART">Kart</option>
                <option value="HAVALE_EFT">Havale / EFT</option>
              </Select>
            </Field>
            <Field label="Kapora (₺)">
              <Input
                type="number"
                inputMode="decimal"
                min="0"
                max={toplam}
                step="0.01"
                placeholder="0"
                value={form.kapora}
                onChange={(e) => set('kapora', tutarSinirla(e.target.value, toplam))}
              />
            </Field>
          </div>

          {form.odemeYontemi === 'HAVALE_EFT' && (
            <Field label="Havale / EFT hesabı">
              <Select value={form.banka} onChange={(e) => set('banka', e.target.value)}>
                {BANKA_HESAPLARI.map((b) => (
                  <option key={b} value={b}>
                    {b}
                  </option>
                ))}
              </Select>
            </Field>
          )}
        </Card>

        <Card className="gap-3.5">
          <h4 className="text-[22px]">Özet</h4>
          <div className="flex justify-between text-[15.5px]">
            <span className="text-ink/55">Saha</span>
            <span>{secilenSaha?.ad || '—'}</span>
          </div>
          <div className="flex justify-between text-[15.5px]">
            <span className="text-ink/55">Tarih</span>
            <span>{uzunTarih(new Date(form.tarih + 'T00:00:00'))}</span>
          </div>
          <div className="flex justify-between text-[15.5px]">
            <span className="text-ink/55">Saat</span>
            <span className="tabular-nums">
              {hh(form.saat)} – {hh(form.saat + form.sure)}
            </span>
          </div>
          <div className="flex justify-between text-[15.5px]">
            <span className="text-ink/55">Saatlik ücret</span>
            <span className="tabular-nums">
              {tl(secilenSaha?.saatlikUcret)} × {form.sure}
            </span>
          </div>
          <div className="h-px bg-divider" />
          <div className="flex items-baseline justify-between">
            <span className="font-heading text-[18px] font-semibold">Ödenecek tutar</span>
            <span className="font-heading text-[32px] font-semibold tabular-nums">{tl(toplam)}</span>
          </div>
          {kapora > 0 && kapora <= toplam && (
            <>
              <div className="flex justify-between text-[15.5px]">
                <span className="text-ink/55">Kapora</span>
                <span className="tabular-nums">{tl(kapora)}</span>
              </div>
              <div className="flex justify-between text-[15.5px]">
                <span className="text-ink/55">Kalan</span>
                <span className="tabular-nums">{tl(toplam - kapora)}</span>
              </div>
            </>
          )}
          <p className="m-0 text-[13.5px] text-ink/55">
            Kafeterya ürünleri rezervasyon kaydedildikten sonra detay ekranından eklenir.
          </p>
          <div className="mt-1 flex gap-2">
            <Button type="submit" variant="primary" className="flex-1" disabled={kaydediyor}>
              {kaydediyor ? 'Kaydediliyor…' : 'Rezervasyonu kaydet'}
            </Button>
            <Button variant="secondary" onClick={() => navigate('/takvim')}>
              Vazgeç
            </Button>
          </div>
        </Card>
      </form>
    </div>
  );
}
