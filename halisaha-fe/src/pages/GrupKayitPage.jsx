import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Field, Input } from '@/components/ui/input';
import { Hata } from '@/components/ui/durum';
import { grupService } from '@/services';
import { apiHata } from '@/lib/api';

export default function GrupKayitPage() {
  const navigate = useNavigate();
  const [ad, setAd] = useState('');
  const [hata, setHata] = useState('');
  const [kaydediyor, setKaydediyor] = useState(false);

  const kaydet = async (e) => {
    e.preventDefault();
    setHata('');
    setKaydediyor(true);
    try {
      await grupService.create({ ad: ad.trim() });
      navigate('/gruplar', { replace: true });
    } catch (err) {
      setHata(apiHata(err));
    } finally {
      setKaydediyor(false);
    }
  };

  return (
    <div>
      <PageHeader kicker="Okul" baslik="Yeni grup" />
      <Hata mesaj={hata} />

      <form onSubmit={kaydet} className="max-w-[460px]">
        <Card className="gap-3.5">
          <h4 className="text-[22px]">Grup bilgileri</h4>
          <Field label="Grup adı">
            <Input
              type="text"
              placeholder="Örn. Minikler (2017-2018)"
              value={ad}
              onChange={(e) => setAd(e.target.value)}
              required
            />
          </Field>
          <div className="mt-1 flex gap-2">
            <Button type="submit" variant="primary" className="flex-1" disabled={kaydediyor}>
              {kaydediyor ? 'Kaydediliyor…' : 'Grubu kaydet'}
            </Button>
            <Button variant="secondary" onClick={() => navigate('/gruplar')}>
              Vazgeç
            </Button>
          </div>
        </Card>
      </form>
    </div>
  );
}
