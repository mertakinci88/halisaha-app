import { useEffect, useState } from 'react';
import { CircleCheck, CirclePause, Save, X } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Field, Input, Select, Textarea, TelInput } from '@/components/ui/input';
import { Seg, SegOpt } from '@/components/ui/seg';
import BilgiIkonu from '@/components/BilgiIkonu';
import { Hata } from '@/components/ui/durum';
import { grupService, ogrenciService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata, useToast } from '@/context/ToastContext';

const BOS = {
  grupId: '',
  adSoyad: '',
  dogumTarih: '',
  veliAdSoyad: '',
  veliTelNo: '',
  durum: 'AKTIF',
  antrenmanGunSayisi: 2,
  not: '',
  odemeAlinir: true,
};

export default function OgrenciKayitPage() {
  const navigate = useNavigate();
  const [gruplar, setGruplar] = useState([]);
  const [form, setForm] = useState(BOS);
  const [hata, setHata] = useHata();
  const toast = useToast();
  const [kaydediyor, setKaydediyor] = useState(false);

  const set = (k, v) => setForm((p) => ({ ...p, [k]: v }));

  useEffect(() => {
    grupService
      .list()
      .then(setGruplar)
      .catch((e) => setHata(apiHata(e), { sessiz: true }));
  }, []);

  const kaydet = async (e) => {
    e.preventDefault();
    setHata('');
    setKaydediyor(true);
    try {
      await ogrenciService.create({
        grupId: form.grupId ? Number(form.grupId) : null,
        adSoyad: form.adSoyad.trim(),
        dogumTarih: form.dogumTarih || null,
        veliAdSoyad: form.veliAdSoyad.trim() || null,
        veliTelNo: form.veliTelNo ? '0' + form.veliTelNo : null,
        durum: form.durum,
        antrenmanGunSayisi: Number(form.antrenmanGunSayisi) || null,
        not: form.not.trim() || null,
        odemeAlinir: form.odemeAlinir,
      });
      toast.basari('Öğrenci kaydedildi.');
      navigate('/ogrenciler', { replace: true });
    } catch (err) {
      setHata(apiHata(err));
    } finally {
      setKaydediyor(false);
    }
  };

  return (
    <div>
      <PageHeader kicker="Okul" baslik="Yeni öğrenci kaydı" />
      <Hata mesaj={hata} />

      <form
        onSubmit={kaydet}
        className="grid items-start gap-5 [grid-template-columns:repeat(auto-fit,minmax(min(300px,100%),1fr))]"
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

              <div className="grid gap-3 min-[430px]:grid-cols-2">
                <Field label="Haftalık antrenman günü">
                  <Select
                    value={form.antrenmanGunSayisi}
                    onChange={(e) => set('antrenmanGunSayisi', Number(e.target.value))}
                  >
                    {[2, 3, 4].map((n) => (
                      <option key={n} value={n}>
                        {n} gün
                      </option>
                    ))}
                  </Select>
                </Field>
                <Field label="Durum">
                  <Seg className="w-full">
                    <SegOpt active={form.durum === 'AKTIF'} tone="basari" onClick={() => set('durum', 'AKTIF')} className="flex-1">
                      <CircleCheck size={16} strokeWidth={1.5} />
                      <span>Aktif</span>
                    </SegOpt>
                    <SegOpt active={form.durum === 'PASIF'} tone="notr" onClick={() => set('durum', 'PASIF')} className="flex-1">
                      <CirclePause size={16} strokeWidth={1.5} />
                      <span>Pasif</span>
                    </SegOpt>
                  </Seg>
                </Field>
              </div>

              <Field label={<>Aylık ödeme<BilgiIkonu metin="Ödeme alınmaz seçilirse öğrenci ödeme durumunda, borçlu listesinde ve ödeme raporlarında yer almaz; hiçbir ay için borç oluşmaz. Sonradan öğrenci detayındaki Aylık ödemeler bölümünden değiştirilebilir." /></>}>
                <Seg className="w-full">
                  <SegOpt active={form.odemeAlinir} tone="basari" onClick={() => set('odemeAlinir', true)} className="flex-1">
                    <span>Ödeme alınır</span>
                  </SegOpt>
                  <SegOpt active={!form.odemeAlinir} tone="notr" onClick={() => set('odemeAlinir', false)} className="flex-1">
                    <span>Ödeme alınmaz</span>
                  </SegOpt>
                </Seg>
              </Field>
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
                  <TelInput value={form.veliTelNo} onChange={(v) => set('veliTelNo', v)} />
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
              <Save size={16} strokeWidth={1.5} />
              <span>{kaydediyor ? 'Kaydediliyor…' : 'Öğrenciyi kaydet'}</span>
            </Button>
            <Button variant="cerceveHata" onClick={() => navigate('/ogrenciler')}>
              <X size={16} strokeWidth={1.5} />
              <span>Vazgeç</span>
            </Button>
          </div>
        </Card>
      </form>
    </div>
  );
}
