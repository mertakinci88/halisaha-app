import { useState } from 'react';
import { Save, X } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Field, Input } from '@/components/ui/input';
import { Hata } from '@/components/ui/durum';
import { urunService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata, useToast } from '@/context/ToastContext';

export default function UrunKayitPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ ad: '', fiyat: '', adet: '', stokUyariAdedi: '10' });
  const [hata, setHata] = useHata();
  const toast = useToast();
  const [kaydediyor, setKaydediyor] = useState(false);

  const set = (k, v) => setForm((p) => ({ ...p, [k]: v }));

  const kaydet = async (e) => {
    e.preventDefault();
    setHata('');
    setKaydediyor(true);
    try {
      await urunService.create({
        ad: form.ad.trim(),
        adet: Number(form.adet),
        stokUyariAdedi: Number(form.stokUyariAdedi),
        fiyat: Number(form.fiyat),
      });
      toast.basari('Ürün eklendi.');
      navigate('/urunler', { replace: true });
    } catch (err) {
      setHata(apiHata(err));
    } finally {
      setKaydediyor(false);
    }
  };

  return (
    <div>
      <PageHeader kicker="Tanımlar" baslik="Yeni ürün" />
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

          <Field label="Stok uyarı adedi">
            <Input
              type="number"
              min="0"
              step="1"
              placeholder="10"
              value={form.stokUyariAdedi}
              onChange={(e) => set('stokUyariAdedi', e.target.value)}
              required
            />
            <p className="m-0 mt-1 text-[13px] text-ink/55">
              Stok bu rakamdan azsa listede ve panelde "Stok az" uyarısı verilir.
            </p>
          </Field>

          <div className="mt-1 flex gap-2">
            <Button type="submit" variant="primary" className="flex-1" disabled={kaydediyor}>
              <Save size={16} strokeWidth={1.5} />
              <span>{kaydediyor ? 'Kaydediliyor…' : 'Ürünü kaydet'}</span>
            </Button>
            <Button variant="cerceveHata" onClick={() => navigate('/urunler')}>
              <X size={16} strokeWidth={1.5} />
              <span>Vazgeç</span>
            </Button>
          </div>
        </Card>
      </form>
    </div>
  );
}
