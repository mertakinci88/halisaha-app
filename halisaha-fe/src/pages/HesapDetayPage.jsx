import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { PageHeader } from '@/components/AppLayout';
import HesapFormu from '@/components/HesapFormu';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import { hesapService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata, useToast } from '@/context/ToastContext';

export default function HesapDetayPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [hesap, setHesap] = useState(null);
  const [hata, setHata] = useHata();
  const toast = useToast();
  const [yukleniyor, setYukleniyor] = useState(true);
  const [kaydediyor, setKaydediyor] = useState(false);

  useEffect(() => {
    hesapService
      .get(id)
      .then(setHesap)
      .catch((e) => setHata(apiHata(e), { sessiz: true }))
      .finally(() => setYukleniyor(false));
  }, [id]);

  const kaydet = async (body) => {
    setHata('');
    setKaydediyor(true);
    try {
      await hesapService.update(id, body);
      toast.basari('Hesap güncellendi.');
      navigate('/hesaplar', { replace: true });
    } catch (err) {
      setHata(apiHata(err));
    } finally {
      setKaydediyor(false);
    }
  };

  if (yukleniyor) return <Yukleniyor />;
  if (!hesap) return <Hata mesaj={hata || 'Hesap bulunamadı.'} />;

  return (
    <div>
      <PageHeader kicker="Tanımlar" baslik="Hesabı güncelle" />
      <Hata mesaj={hata} />
      <HesapFormu
        baslangic={{ ad: hesap.ad, iban: hesap.iban, hesapSahibi: hesap.hesapSahibi, durum: hesap.durum }}
        kaydediyor={kaydediyor}
        onKaydet={kaydet}
        kaydetEtiket="Hesabı güncelle"
      />
    </div>
  );
}
