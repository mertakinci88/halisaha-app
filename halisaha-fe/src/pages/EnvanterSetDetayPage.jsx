import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { PageHeader } from '@/components/AppLayout';
import EnvanterSetFormu from '@/components/EnvanterSetFormu';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import { envanterSetService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata, useToast } from '@/context/ToastContext';

export default function EnvanterSetDetayPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [set, setSet] = useState(null);
  const [hata, setHata] = useHata();
  const toast = useToast();
  const [yukleniyor, setYukleniyor] = useState(true);
  const [kaydediyor, setKaydediyor] = useState(false);

  useEffect(() => {
    envanterSetService
      .get(id)
      .then(setSet)
      .catch((e) => setHata(apiHata(e), { sessiz: true }))
      .finally(() => setYukleniyor(false));
  }, [id]);

  const kaydet = async (govde) => {
    setHata('');
    setKaydediyor(true);
    try {
      await envanterSetService.update(id, govde);
      toast.basari('Set güncellendi.');
      navigate('/envanter?sekme=setler', { replace: true });
    } catch (err) {
      setHata(apiHata(err));
    } finally {
      setKaydediyor(false);
    }
  };

  if (yukleniyor) return <Yukleniyor />;
  if (!set) return <Hata mesaj={hata || 'Set bulunamadı.'} />;

  return (
    <div>
      <PageHeader kicker="Tanımlar" baslik="Seti güncelle" />
      <Hata mesaj={hata} />
      <EnvanterSetFormu
        baslangic={{ ad: set.ad, envanterIdleri: set.kalemler.map((k) => k.envanterId) }}
        kaydetEtiketi="Seti güncelle"
        kaydediyor={kaydediyor}
        onKaydet={kaydet}
      />
    </div>
  );
}
