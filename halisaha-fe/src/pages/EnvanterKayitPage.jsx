import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '@/components/AppLayout';
import EnvanterFormu from '@/components/EnvanterFormu';
import { Hata } from '@/components/ui/durum';
import { envanterService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata, useToast } from '@/context/ToastContext';

export default function EnvanterKayitPage() {
  const navigate = useNavigate();
  const [hata, setHata] = useHata();
  const toast = useToast();
  const [kaydediyor, setKaydediyor] = useState(false);

  const kaydet = async (govde) => {
    setHata('');
    setKaydediyor(true);
    try {
      await envanterService.create(govde);
      toast.basari('Envanter eklendi.');
      navigate('/envanter', { replace: true });
    } catch (err) {
      setHata(apiHata(err));
    } finally {
      setKaydediyor(false);
    }
  };

  return (
    <div>
      <PageHeader kicker="Tanımlar" baslik="Yeni envanter" />
      <Hata mesaj={hata} />
      <EnvanterFormu kaydetEtiketi="Envanteri kaydet" kaydediyor={kaydediyor} onKaydet={kaydet} />
    </div>
  );
}
