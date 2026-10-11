import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { PageHeader } from '@/components/AppLayout';
import EnvanterFormu from '@/components/EnvanterFormu';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import { envanterService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata, useToast } from '@/context/ToastContext';

export default function EnvanterDetayPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [envanter, setEnvanter] = useState(null);
  const [hata, setHata] = useHata();
  const toast = useToast();
  const [yukleniyor, setYukleniyor] = useState(true);
  const [kaydediyor, setKaydediyor] = useState(false);

  useEffect(() => {
    envanterService
      .get(id)
      .then(setEnvanter)
      .catch((e) => setHata(apiHata(e), { sessiz: true }))
      .finally(() => setYukleniyor(false));
  }, [id]);

  const kaydet = async (govde) => {
    setHata('');
    setKaydediyor(true);
    try {
      await envanterService.update(id, govde);
      toast.basari('Envanter güncellendi.');
      navigate('/envanter', { replace: true });
    } catch (err) {
      setHata(apiHata(err));
    } finally {
      setKaydediyor(false);
    }
  };

  if (yukleniyor) return <Yukleniyor />;
  if (!envanter) return <Hata mesaj={hata || 'Envanter bulunamadı.'} />;

  return (
    <div>
      <PageHeader kicker="Tanımlar" baslik="Envanteri güncelle" />
      <Hata mesaj={hata} />
      <EnvanterFormu
        baslangic={{
          tur: envanter.tur,
          beden: envanter.bedenli ? envanter.beden : '',
          satisFiyati: String(envanter.satisFiyati),
          adet: String(envanter.adet),
          stokUyariAdedi: String(envanter.stokUyariAdedi),
        }}
        kaydetEtiketi="Envanteri güncelle"
        kaydediyor={kaydediyor}
        onKaydet={kaydet}
      />
    </div>
  );
}
