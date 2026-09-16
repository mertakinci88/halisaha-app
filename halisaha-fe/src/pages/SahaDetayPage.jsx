import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Field, Input } from '@/components/ui/input';
import { Seg, SegOpt } from '@/components/ui/seg';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import { ConfirmDialog } from '@/components/ui/dialog';
import { rezervasyonService, sahaService } from '@/services';
import { apiHata } from '@/lib/api';

export default function SahaDetayPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [form, setForm] = useState({ ad: '', saatlikUcret: '', durum: 'AKTIF' });
  const [orijinalDurum, setOrijinalDurum] = useState('AKTIF');
  const [hata, setHata] = useState('');
  const [yukleniyor, setYukleniyor] = useState(true);
  const [kaydediyor, setKaydediyor] = useState(false);
  const [pasifUyariTuru, setPasifUyariTuru] = useState(null);

  useEffect(() => {
    sahaService
      .get(id)
      .then((s) => {
        setForm({ ad: s.ad, saatlikUcret: String(s.saatlikUcret), durum: s.durum });
        setOrijinalDurum(s.durum);
      })
      .catch((e) => setHata(apiHata(e)))
      .finally(() => setYukleniyor(false));
  }, [id]);

  const set = (k, v) => setForm((p) => ({ ...p, [k]: v }));

  const kaydetGonder = async () => {
    setHata('');
    setKaydediyor(true);
    try {
      await sahaService.update(id, {
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

  const kaydet = async (e) => {
    e.preventDefault();
    if (form.durum === 'PASIF' && orijinalDurum !== 'PASIF') {
      setHata('');
      setKaydediyor(true);
      try {
        const rezervasyonlar = await rezervasyonService.list({ sahaId: id });
        if (rezervasyonlar.some((r) => r.durum === 'AKTIF')) {
          setPasifUyariTuru('aktif');
          return;
        } else if (rezervasyonlar.length > 0) {
          setPasifUyariTuru('gecmis');
          return;
        }
      } catch (err) {
        setHata(apiHata(err));
        return;
      } finally {
        setKaydediyor(false);
      }
    }
    await kaydetGonder();
  };

  if (yukleniyor) return <Yukleniyor />;

  return (
    <div>
      <PageHeader kicker="Tanımlar" baslik="Sahayı güncelle" />
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
              {kaydediyor ? 'Kaydediliyor…' : 'Sahayı güncelle'}
            </Button>
            <Button variant="secondary" onClick={() => navigate('/sahalar')}>
              Vazgeç
            </Button>
          </div>
        </Card>
      </form>

      <ConfirmDialog
        open={!!pasifUyariTuru}
        title={
          pasifUyariTuru === 'aktif'
            ? 'Bu sahaya ait rezervasyonlar bulunuyor'
            : 'Bu sahaya ait geçmiş rezervasyon kayıtları bulunuyor'
        }
        description={
          pasifUyariTuru === 'aktif'
            ? 'Bu sahayı pasife almanız durumunda rezervasyon ve takvim ekranlarında gösterilmeyecektir. Emin misiniz?'
            : 'Bu sahayı pasife almak istediğinize emin misiniz?'
        }
        isim={form.ad}
        confirmLabel="Pasife al"
        onConfirm={() => {
          setPasifUyariTuru(null);
          kaydetGonder();
        }}
        onCancel={() => setPasifUyariTuru(null)}
      />
    </div>
  );
}
