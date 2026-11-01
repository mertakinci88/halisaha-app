import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { PageHeader } from '@/components/AppLayout';
import EnvanterTurFormu from '@/components/EnvanterTurFormu';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import { envanterTurService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata, useToast } from '@/context/ToastContext';

export default function EnvanterTurDetayPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [tur, setTur] = useState(null);
  const [hata, setHata] = useHata();
  const toast = useToast();
  const [yukleniyor, setYukleniyor] = useState(true);
  const [kaydediyor, setKaydediyor] = useState(false);

  useEffect(() => {
    envanterTurService
      .list()
      .then((l) => setTur(l.find((t) => String(t.id) === id) || null))
      .catch((e) => setHata(apiHata(e), { sessiz: true }))
      .finally(() => setYukleniyor(false));
  }, [id]);

  const kaydet = async (govde) => {
    setHata('');
    setKaydediyor(true);
    try {
      await envanterTurService.update(id, govde);
      toast.basari('Envanter türü güncellendi.');
      navigate('/envanter?sekme=turler', { replace: true });
    } catch (err) {
      setHata(apiHata(err));
    } finally {
      setKaydediyor(false);
    }
  };

  if (yukleniyor) return <Yukleniyor />;
  if (!tur) return <Hata mesaj={hata || 'Envanter türü bulunamadı.'} />;

  return (
    <div>
      <PageHeader kicker="Tanımlar" baslik="Envanter türünü güncelle" />
      <Hata mesaj={hata} />
      <EnvanterTurFormu baslangic={tur} kilitli={tur.kullanimda} kaydetEtiketi="Türü güncelle" kaydediyor={kaydediyor} onKaydet={kaydet} />
    </div>
  );
}
