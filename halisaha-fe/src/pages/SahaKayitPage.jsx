import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Field, Input } from '@/components/ui/input';
import { Seg, SegOpt } from '@/components/ui/seg';
import { Hata } from '@/components/ui/durum';
import { sahaService } from '@/services';
import { apiHata } from '@/lib/api';

export default function SahaKayitPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ ad: '', saatlikUcret: '', durum: 'AKTIF' });
  const [hata, setHata] = useState('');
  const [kaydediyor, setKaydediyor] = useState(false);

  const set = (k, v) => setForm((p) => ({ ...p, [k]: v }));

  const kaydet = async (e) => {
    e.preventDefault();
    setHata('');
    setKaydediyor(true);
    try {
      await sahaService.create({
        ad: form.ad.trim(),
        durum: form.durum,
        saatlikUcret: Number(form.saatlikUcret),
      });
      navigate('/sahalar', { replace: true });
    } catch (err) {
      setHata(apiHata(err));
    } finally {
      setKaydediyor(false);
    }
  };

  return (
    <div>
      <PageHeader kicker="Tanımlar" baslik="Yeni saha" />
      <Hata mesaj={hata} />

      <form onSubmit={kaydet} className="max-w-[560px]">
        <Card className="gap-3.5">
          <h4 className="text-[22px]">Saha bilgileri</h4>

          <Field label="Saha adı">
            <Input
              type="text"
              placeholder="Örn. Saha 1"
              value={form.ad}
              onChange={(e) => set('ad', e.target.value)}
              required
            />
          </Field>

          <div className="grid grid-cols-2 gap-3">
            <Field label="Saatlik ücret">
              <Input
                type="number"
                placeholder="900"
                value={form.saatlikUcret}
                onChange={(e) => set('saatlikUcret', e.target.value)}
                required
              />
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

          <div className="mt-1 flex gap-2">
            <Button type="submit" variant="primary" className="flex-1" disabled={kaydediyor}>
              {kaydediyor ? 'Kaydediliyor…' : 'Sahayı kaydet'}
            </Button>
            <Button variant="secondary" onClick={() => navigate('/sahalar')}>
              Vazgeç
            </Button>
          </div>
        </Card>
      </form>
    </div>
  );
}
