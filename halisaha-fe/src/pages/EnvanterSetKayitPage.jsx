import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '@/components/AppLayout';
import EnvanterSetFormu from '@/components/EnvanterSetFormu';
import { Hata } from '@/components/ui/durum';
import { envanterSetService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata, useToast } from '@/context/ToastContext';

export default function EnvanterSetKayitPage() {
  const navigate = useNavigate();
  const [hata, setHata] = useHata();
  const toast = useToast();
  const [kaydediyor, setKaydediyor] = useState(false);

  const kaydet = async (govde) => {
    setHata('');
    setKaydediyor(true);
    try {
      await envanterSetService.create(govde);
      toast.basari('Set eklendi.');
      navigate('/envanter?sekme=setler', { replace: true });
    } catch (err) {
      setHata(apiHata(err));
    } finally {
      setKaydediyor(false);
    }
  };

  return (
    <div>
      <PageHeader kicker="Tanımlar" baslik="Yeni set" />
      <Hata mesaj={hata} />
      <EnvanterSetFormu kaydetEtiketi="Seti kaydet" kaydediyor={kaydediyor} onKaydet={kaydet} />
    </div>
  );
}
