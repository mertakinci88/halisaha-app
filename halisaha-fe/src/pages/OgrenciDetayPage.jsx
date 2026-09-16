import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Field, Input, Select, Textarea } from '@/components/ui/input';
import { Seg, SegOpt } from '@/components/ui/seg';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import { grupService, ogrenciService } from '@/services';
import { apiHata } from '@/lib/api';

const BOS = {
  grupId: '',
  adSoyad: '',
  dogumTarih: '',
  veliAdSoyad: '',
  veliTelNo: '',
  durum: 'AKTIF',
  antrenmanGunSayisi: 2,
  aylikAidat: '',
  odemeDurumu: 'ODENMEDI',
  odemePlani: 'Aylık',
  odenenDonem: '',
  odemeTarihi: '',
  sonrakiOdemeTarih: '',
  odenenTutar: '',
  odemeSekli: 'NAKIT',
  not: '',
};

const tarihGirdisi = (v) => (v ? String(v).slice(0, 10) : '');

export default function OgrenciDetayPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [gruplar, setGruplar] = useState([]);
  const [form, setForm] = useState(BOS);
  const [hata, setHata] = useState('');
  const [yukleniyor, setYukleniyor] = useState(true);
  const [kaydediyor, setKaydediyor] = useState(false);

  const set = (k, v) => setForm((p) => ({ ...p, [k]: v }));

  useEffect(() => {
    grupService
      .list()
      .then(setGruplar)
      .catch((e) => setHata(apiHata(e)));
  }, []);

  useEffect(() => {
    ogrenciService
      .get(id)
      .then((o) =>
        setForm({
          grupId: o.grupId ? String(o.grupId) : '',
          adSoyad: o.adSoyad || '',
          dogumTarih: tarihGirdisi(o.dogumTarih),
          veliAdSoyad: o.veliAdSoyad || '',
          veliTelNo: o.veliTelNo || '',
          durum: o.durum || 'AKTIF',
          antrenmanGunSayisi: o.antrenmanGunSayisi || 2,
          aylikAidat: o.aylikAidat != null ? String(o.aylikAidat) : '',
          odemeDurumu: o.odemeDurumu || 'ODENMEDI',
          odemePlani: o.odemePlani || 'Aylık',
          odenenDonem: o.odenenDonem || '',
          odemeTarihi: tarihGirdisi(o.odemeTarihi),
          sonrakiOdemeTarih: tarihGirdisi(o.sonrakiOdemeTarih),
          odenenTutar: o.odenenTutar != null ? String(o.odenenTutar) : '',
          odemeSekli: o.odemeSekli || 'NAKIT',
          not: o.not || '',
        }),
      )
      .catch((e) => setHata(apiHata(e)))
      .finally(() => setYukleniyor(false));
  }, [id]);

  const kaydet = async (e) => {
    e.preventDefault();
    setHata('');
    setKaydediyor(true);
    try {
      await ogrenciService.update(id, {
        grupId: form.grupId ? Number(form.grupId) : null,
        adSoyad: form.adSoyad.trim(),
        dogumTarih: form.dogumTarih || null,
        veliAdSoyad: form.veliAdSoyad.trim() || null,
        veliTelNo: form.veliTelNo.trim() || null,
        durum: form.durum,
        antrenmanGunSayisi: Number(form.antrenmanGunSayisi) || null,
        aylikAidat: form.aylikAidat ? Number(form.aylikAidat) : null,
        odemeDurumu: form.odemeDurumu,
        odemePlani: form.odemePlani || null,
        odenenDonem: form.odenenDonem || null,
        odemeTarihi: form.odemeTarihi || null,
        sonrakiOdemeTarih: form.sonrakiOdemeTarih || null,
        odenenTutar: form.odenenTutar ? Number(form.odenenTutar) : null,
        odemeSekli: form.odemeSekli,
        not: form.not.trim() || null,
      });
      navigate('/ogrenciler', { replace: true });
    } catch (err) {
      setHata(apiHata(err));
    } finally {
      setKaydediyor(false);
    }
  };

  if (yukleniyor) return <Yukleniyor />;

  return (
    <div>
      <PageHeader kicker="Okul" baslik="Öğrenciyi güncelle" />
      <Hata mesaj={hata} />

      <form
        onSubmit={kaydet}
        className="grid items-start gap-5 [grid-template-columns:repeat(auto-fit,minmax(300px,1fr))]"
      >
        <Card className="gap-3.5">
          <h4 className="text-[22px]">Öğrenci bilgileri</h4>

          <Field label="Ad soyad">
            <Input
              type="text"
              placeholder="Örn. Ali Demir"
              value={form.adSoyad}
              onChange={(e) => set('adSoyad', e.target.value)}
              required
            />
          </Field>

          <div className="grid grid-cols-2 gap-3">
            <Field label="Doğum tarihi">
              <Input type="date" value={form.dogumTarih} onChange={(e) => set('dogumTarih', e.target.value)} />
            </Field>
            <Field label="Grup">
              <Select value={form.grupId} onChange={(e) => set('grupId', e.target.value)}>
                <option value="">Grup seçilmedi</option>
                {gruplar.map((g) => (
                  <option key={g.id} value={g.id}>
                    {g.ad}
                  </option>
                ))}
              </Select>
            </Field>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Field label="Haftalık antrenman günü">
              <Select
                value={form.antrenmanGunSayisi}
                onChange={(e) => set('antrenmanGunSayisi', Number(e.target.value))}
              >
                {[1, 2, 3, 4, 5].map((n) => (
                  <option key={n} value={n}>
                    {n} gün
                  </option>
                ))}
              </Select>
            </Field>
            <Field label="Durum">
              <Seg className="w-full">
                <SegOpt active={form.durum === 'AKTIF'} onClick={() => set('durum', 'AKTIF')} className="flex-1">
                  Aktif
                </SegOpt>
                <SegOpt active={form.durum === 'PASIF'} onClick={() => set('durum', 'PASIF')} className="flex-1">
                  Pasif
                </SegOpt>
              </Seg>
            </Field>
          </div>

          <div className="my-0.5 h-px bg-divider" />
          <h4 className="text-[22px]">Veli bilgileri</h4>

          <div className="grid grid-cols-2 gap-3">
            <Field label="Veli ad soyad">
              <Input
                type="text"
                placeholder="Örn. Hakan Demir"
                value={form.veliAdSoyad}
                onChange={(e) => set('veliAdSoyad', e.target.value)}
              />
            </Field>
            <Field label="Veli telefon">
              <Input
                type="tel"
                placeholder="05__ ___ __ __"
                value={form.veliTelNo}
                onChange={(e) => set('veliTelNo', e.target.value)}
              />
            </Field>
          </div>

          <Field label="Not">
            <Textarea
              placeholder="Sağlık durumu, izin bilgisi vb."
              value={form.not}
              onChange={(e) => set('not', e.target.value)}
            />
          </Field>
        </Card>

        <Card className="gap-3.5">
          <h4 className="text-[22px]">Ödeme bilgileri</h4>

          <div className="grid grid-cols-2 gap-3">
            <Field label="Aylık aidat">
              <Input
                type="number"
                min="0"
                step="50"
                placeholder="1500"
                value={form.aylikAidat}
                onChange={(e) => set('aylikAidat', e.target.value)}
              />
            </Field>
            <Field label="Ödeme planı">
              <Select value={form.odemePlani} onChange={(e) => set('odemePlani', e.target.value)}>
                <option value="Aylık">Aylık</option>
                <option value="3 aylık">3 aylık</option>
                <option value="Yıllık">Yıllık</option>
              </Select>
            </Field>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Field label="Ödeme şekli">
              <Select value={form.odemeSekli} onChange={(e) => set('odemeSekli', e.target.value)}>
                <option value="NAKIT">Nakit</option>
                <option value="KART">Kart</option>
                <option value="HAVALE_EFT">Havale / EFT</option>
              </Select>
            </Field>
            <Field label="Ödeme durumu">
              <Select value={form.odemeDurumu} onChange={(e) => set('odemeDurumu', e.target.value)}>
                <option value="ODENMEDI">Ödenmedi</option>
                <option value="ODENDI">Ödendi</option>
                <option value="KISMI_ODENDI">Kısmi ödendi</option>
              </Select>
            </Field>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Field label="Ödenen dönem">
              <Input
                type="text"
                placeholder="Eylül 2026"
                value={form.odenenDonem}
                onChange={(e) => set('odenenDonem', e.target.value)}
              />
            </Field>
            <Field label="Ödenen tutar">
              <Input
                type="number"
                min="0"
                step="50"
                placeholder="1500"
                value={form.odenenTutar}
                onChange={(e) => set('odenenTutar', e.target.value)}
              />
            </Field>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Field label="Ödeme tarihi">
              <Input type="date" value={form.odemeTarihi} onChange={(e) => set('odemeTarihi', e.target.value)} />
            </Field>
            <Field label="Sonraki ödeme tarihi">
              <Input
                type="date"
                value={form.sonrakiOdemeTarih}
                onChange={(e) => set('sonrakiOdemeTarih', e.target.value)}
              />
            </Field>
          </div>

          <div className="mt-1.5 flex gap-2">
            <Button type="submit" variant="primary" className="flex-1" disabled={kaydediyor}>
              {kaydediyor ? 'Kaydediliyor…' : 'Öğrenciyi güncelle'}
            </Button>
            <Button variant="secondary" onClick={() => navigate('/ogrenciler')}>
              Vazgeç
            </Button>
          </div>
        </Card>
      </form>
    </div>
  );
}
