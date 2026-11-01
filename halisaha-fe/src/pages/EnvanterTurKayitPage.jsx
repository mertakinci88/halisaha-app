import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { PageHeader } from '@/components/AppLayout';
import EnvanterTurFormu from '@/components/EnvanterTurFormu';
import { Hata } from '@/components/ui/durum';
import { envanterTurService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata, useToast } from '@/context/ToastContext';

export default function EnvanterTurKayitPage() {
  const navigate = useNavigate();
  const { state } = useLocation();
  const [hata, setHata] = useHata();
  const toast = useToast();
  const [kaydediyor, setKaydediyor] = useState(false);

  const kaydet = async (govde) => {
    setHata('');
    setKaydediyor(true);
    try {
      const yeni = await envanterTurService.create(govde);
      toast.basari('Envanter türü eklendi.');
      // Yeni envanter formundan gelindiyse forma dönülür ve yeni tür seçili gelir
      if (state?.donus) navigate(state.donus, { replace: true, state: { form: state.form, turId: yeni.id } });
      else navigate('/envanter?sekme=turler', { replace: true });
    } catch (err) {
      setHata(apiHata(err));
    } finally {
      setKaydediyor(false);
    }
  };

  return (
    <div>
      <PageHeader kicker="Tanımlar" baslik="Yeni envanter türü" />
      <Hata mesaj={hata} />
      <EnvanterTurFormu kaydetEtiketi="Türü kaydet" kaydediyor={kaydediyor} onKaydet={kaydet} />
    </div>
  );
}
