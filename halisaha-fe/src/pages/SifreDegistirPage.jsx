import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Field, Input } from '@/components/ui/input';
import { Hata } from '@/components/ui/durum';
import { authService } from '@/services';
import { apiHata } from '@/lib/api';
import { useAuth } from '@/context/AuthContext';
import { useHata, useToast } from '@/context/ToastContext';

export default function SifreDegistirPage() {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ eskiSifre: '', yeniSifre: '', yeniSifreTekrar: '' });
  const [hata, setHata] = useHata();
  const toast = useToast();
  const [kaydediyor, setKaydediyor] = useState(false);
  const [tamamlandi, setTamamlandi] = useState(false);
  const zamanlayici = useRef(null);

  useEffect(() => () => clearTimeout(zamanlayici.current), []);

  const alan = (ad) => (e) => setForm((p) => ({ ...p, [ad]: e.target.value }));

  const gonder = async (e) => {
    e.preventDefault();
    setHata('');
    if (form.yeniSifre !== form.yeniSifreTekrar) {
      setHata('Yeni şifre ile tekrarı aynı olmalıdır');
      return;
    }
    setKaydediyor(true);
    try {
      await authService.sifreDegistir(form.eskiSifre, form.yeniSifre);
      // Tüm oturumlar kapandı; mesajı gösterip kısa süre sonra yeniden giriş ekranına yönlendir.
      // logout() girişli durumu hemen düşürdüğü için çağrısı bekleme sonrasına bırakıldı.
      setTamamlandi(true);
      toast.basari('Şifreniz değiştirildi. Giriş ekranına yönlendiriliyorsunuz…');
      zamanlayici.current = setTimeout(async () => {
        await logout();
        navigate('/giris', { replace: true });
      }, 2000);
    } catch (err) {
      setHata(apiHata(err));
      setKaydediyor(false);
    }
  };

  return (
    <div>
      <PageHeader kicker="Hesabım" baslik="Şifre değiştir" />
      <Hata mesaj={hata} />

      <form onSubmit={gonder} className="max-w-[420px]">
        <Card className="gap-3.5">
          <p className="m-0 text-[14.5px] text-ink/55">
            Şifre değiştiğinde tüm oturumlarınız kapanır ve yeni şifrenizle yeniden giriş yaparsınız.
          </p>
          <Field label="Mevcut şifre">
            <Input
              type="password"
              autoComplete="current-password"
              value={form.eskiSifre}
              onChange={alan('eskiSifre')}
              required
            />
          </Field>
          <Field label="Yeni şifre">
            <Input
              type="password"
              autoComplete="new-password"
              minLength={6}
              value={form.yeniSifre}
              onChange={alan('yeniSifre')}
              required
            />
          </Field>
          <Field label="Yeni şifre (tekrar)">
            <Input
              type="password"
              autoComplete="new-password"
              minLength={6}
              value={form.yeniSifreTekrar}
              onChange={alan('yeniSifreTekrar')}
              required
            />
          </Field>
          <div className="mt-1 flex gap-2">
            <Button type="submit" variant="primary" className="flex-1" disabled={kaydediyor || tamamlandi}>
              {tamamlandi ? 'Yönlendiriliyor…' : kaydediyor ? 'Kaydediliyor…' : 'Şifreyi değiştir'}
            </Button>
            <Button variant="secondary" disabled={tamamlandi} onClick={() => navigate(-1)}>
              Vazgeç
            </Button>
          </div>
        </Card>
      </form>
    </div>
  );
}
