import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Field, Input } from '@/components/ui/input';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import { urunService } from '@/services';
import { apiHata } from '@/lib/api';

export default function UrunDetayPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [form, setForm] = useState({ ad: '', fiyat: '', adet: '' });
  const [hata, setHata] = useState('');
  const [yukleniyor, setYukleniyor] = useState(true);
  const [kaydediyor, setKaydediyor] = useState(false);

  useEffect(() => {
    urunService
      .get(id)
      .then((u) => setForm({ ad: u.ad, fiyat: String(u.fiyat), adet: String(u.adet) }))
      .catch((e) => setHata(apiHata(e)))
      .finally(() => setYukleniyor(false));
  }, [id]);

  const set = (k, v) => setForm((p) => ({ ...p, [k]: v }));

  const kaydet = async (e) => {
    e.preventDefault();
    setHata('');
    setKaydediyor(true);
    try {
      await urunService.update(id, {
        ad: form.ad.trim(),
        adet: Number(form.adet),
        fiyat: Number(form.fiyat),
      });
      navigate('/urunler', { replace: true });
    } catch (err) {
      setHata(apiHata(err));
    } finally {
      setKaydediyor(false);
    }
  };

  if (yukleniyor) return <Yukleniyor />;

  return (
    <div>
      <PageHeader kicker="Tanımlar" baslik="Ürünü güncelle" />
      <Hata mesaj={hata} />

      <form onSubmit={kaydet} className="max-w-[560px]">
        <Card className="gap-3.5">
          <h4 className="text-[22px]">Ürün bilgileri</h4>

          <Field label="Ürün adı">
            <Input
              type="text"
              placeholder="Örn. Limonata"
              value={form.ad}
              onChange={(e) => set('ad', e.target.value)}
              required
            />
          </Field>

          <div className="grid grid-cols-2 gap-3">
            <Field label="Fiyat">
              <Input
                type="number"
                min="1"
                step="1"
                placeholder="25"
                value={form.fiyat}
                onChange={(e) => set('fiyat', e.target.value)}
                required
              />
            </Field>
            <Field label="Adet">
              <Input
                type="number"
                min="0"
                step="1"
                placeholder="40"
                value={form.adet}
                onChange={(e) => set('adet', e.target.value)}
                required
              />
            </Field>
          </div>

          <div className="mt-1 flex gap-2">
            <Button type="submit" variant="primary" className="flex-1" disabled={kaydediyor}>
              {kaydediyor ? 'Kaydediliyor…' : 'Ürünü güncelle'}
            </Button>
            <Button variant="secondary" onClick={() => navigate('/urunler')}>
              Vazgeç
            </Button>
          </div>
        </Card>
      </form>
    </div>
  );
}
