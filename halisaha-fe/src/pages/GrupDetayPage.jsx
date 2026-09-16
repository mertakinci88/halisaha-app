import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Field, Input } from '@/components/ui/input';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import { grupService } from '@/services';
import { apiHata } from '@/lib/api';

export default function GrupDetayPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [ad, setAd] = useState('');
  const [hata, setHata] = useState('');
  const [yukleniyor, setYukleniyor] = useState(true);
  const [kaydediyor, setKaydediyor] = useState(false);

  useEffect(() => {
    grupService
      .get(id)
      .then((g) => setAd(g.ad))
      .catch((e) => setHata(apiHata(e)))
      .finally(() => setYukleniyor(false));
  }, [id]);

  const kaydet = async (e) => {
    e.preventDefault();
    setHata('');
    setKaydediyor(true);
    try {
      await grupService.update(id, { ad: ad.trim() });
      navigate('/gruplar', { replace: true });
    } catch (err) {
      setHata(apiHata(err));
    } finally {
      setKaydediyor(false);
    }
  };

  if (yukleniyor) return <Yukleniyor />;

  return (
    <div>
      <PageHeader kicker="Okul" baslik="Grubu güncelle" />
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
              {kaydediyor ? 'Kaydediliyor…' : 'Grubu güncelle'}
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
