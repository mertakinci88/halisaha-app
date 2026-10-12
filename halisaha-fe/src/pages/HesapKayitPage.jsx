import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '@/components/AppLayout';
import HesapFormu from '@/components/HesapFormu';
import { Hata } from '@/components/ui/durum';
import { hesapService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata, useToast } from '@/context/ToastContext';

export default function HesapKayitPage() {
  const navigate = useNavigate();
  const [hata, setHata] = useHata();
  const toast = useToast();
  const [kaydediyor, setKaydediyor] = useState(false);

  const kaydet = async (body) => {
    setHata('');
    setKaydediyor(true);
    try {
      await hesapService.create(body);
      toast.basari('Hesap oluşturuldu.');
      navigate('/hesaplar', { replace: true });
    } catch (err) {
      setHata(apiHata(err));
    } finally {
      setKaydediyor(false);
    }
  };

  return (
    <div>
      <PageHeader kicker="Tanımlar" baslik="Yeni hesap" />
      <Hata mesaj={hata} />
      <HesapFormu
        baslangic={{ ad: '', iban: '', hesapSahibi: '', durum: 'AKTIF' }}
        kaydediyor={kaydediyor}
        onKaydet={kaydet}
        kaydetEtiket="Hesabı kaydet"
      />
    </div>
  );
}
