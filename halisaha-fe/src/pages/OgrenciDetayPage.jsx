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
import { useHata, useToast } from '@/context/ToastContext';
import { useAuth } from '@/context/AuthContext';
import EnvanterSatisKarti from '@/components/EnvanterSatisKarti';
import OgrenciOdemeKarti from '@/components/OgrenciOdemeKarti';

const BOS = {
  grupId: '',
  adSoyad: '',
  dogumTarih: '',
  veliAdSoyad: '',
  veliTelNo: '',
  durum: 'AKTIF',
  antrenmanGunSayisi: 2,
  not: '',
};

const tarihGirdisi = (v) => (v ? String(v).slice(0, 10) : '');

export default function OgrenciDetayPage() {
  const { yetkiVar } = useAuth();
  const { id } = useParams();
  const navigate = useNavigate();
  const [gruplar, setGruplar] = useState([]);
  const [form, setForm] = useState(BOS);
  const [hata, setHata] = useHata();
  const toast = useToast();
  const [yukleniyor, setYukleniyor] = useState(true);
  const [kaydediyor, setKaydediyor] = useState(false);

  const set = (k, v) => setForm((p) => ({ ...p, [k]: v }));

  useEffect(() => {
    grupService
      .list()
      .then(setGruplar)
      .catch((e) => setHata(apiHata(e), { sessiz: true }));
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
          not: o.not || '',
        }),
      )
      .catch((e) => setHata(apiHata(e), { sessiz: true }))
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
        not: form.not.trim() || null,
      });
      toast.basari('Öğrenci bilgileri güncellendi.');
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
      <PageHeader kicker="Okul" baslik="Öğrenci detay" />
      <Hata mesaj={hata} />

      <form
        onSubmit={kaydet}
        className="grid items-start gap-5 [grid-template-columns:repeat(auto-fit,minmax(300px,1fr))]"
      >
        <Card className="gap-3.5">
          <div className="grid gap-x-10 gap-y-5 md:grid-cols-2">
            <div className="grid content-start gap-3.5">
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
                    {[...new Set([2, 3, 4, Number(form.antrenmanGunSayisi)])].sort().map((n) => (
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
            </div>
            <div className="grid content-start gap-3.5 border-t border-divider pt-5 md:border-t-0 md:pt-0">
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
            </div>
          </div>

          <div className="mt-1.5 flex gap-2">
            <Button type="submit" variant="primary" className="flex-1 md:flex-none" disabled={kaydediyor}>
              {kaydediyor ? 'Kaydediliyor…' : 'Kaydet'}
            </Button>
            <Button variant="secondary" onClick={() => navigate('/ogrenciler')}>
              Vazgeç
            </Button>
          </div>
        </Card>
      </form>

      <OgrenciOdemeKarti ogrenciId={id} />

      {/* Envanter seti satışı: yalnızca ADMIN ya da Envanter yetkisi olanlar görür */}
      {yetkiVar('ENVANTER') ? <EnvanterSatisKarti ogrenciId={id} /> : null}
    </div>
  );
}
