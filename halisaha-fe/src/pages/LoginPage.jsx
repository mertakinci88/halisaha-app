import { useState } from 'react';
import { LogIn } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import AltBilgi from '@/components/AltBilgi';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Field, Input } from '@/components/ui/input';
import { useAuth } from '@/context/AuthContext';
import { apiHata } from '@/lib/api';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [kullaniciAdi, setKullaniciAdi] = useState('');
  const [sifre, setSifre] = useState('');
  const [hata, setHata] = useState('');
  const [bekliyor, setBekliyor] = useState(false);

  const gonder = async (e) => {
    e.preventDefault();
    setHata('');
    setBekliyor(true);
    try {
      await login(kullaniciAdi.trim(), sifre);
      navigate('/panel', { replace: true });
    } catch (err) {
      setHata(apiHata(err));
    } finally {
      setBekliyor(false);
    }
  };

  return (
    <div className="flex min-h-full flex-col bg-bg">
      <div className="grid flex-1 place-items-center px-5 py-10">
        <div className="w-full max-w-[380px]">
          <div className="mb-7 flex items-baseline gap-2.5">
            <span className="font-heading text-[30px] font-semibold tracking-[-0.01em]">HALI SAHA</span>
            <span className="font-heading text-[30px] text-accent">YÖNETİM</span>
          </div>
  
          <Card className="gap-4 p-6">
            <div>
              <h4 className="mb-0.5 text-[22px]">Giriş yap</h4>
              <p className="m-0 text-[14.5px] text-ink/55">Devam etmek için hesabınıza giriş yapın.</p>
            </div>
  
            <form onSubmit={gonder} className="flex flex-col gap-4">
              <Field label="Kullanıcı adı">
                <Input
                  type="text"
                  autoComplete="username"
                  value={kullaniciAdi}
                  onChange={(e) => setKullaniciAdi(e.target.value)}
                  required
                />
              </Field>
              <Field label="Şifre">
                <Input
                  type="password"
                  autoComplete="current-password"
                  value={sifre}
                  onChange={(e) => setSifre(e.target.value)}
                  required
                />
              </Field>
  
              {hata ? <div className="text-[14.5px] text-accent-800">{hata}</div> : null}
  
              <Button type="submit" variant="primary" size="block" disabled={bekliyor}>
                <LogIn size={16} strokeWidth={1.5} />
                <span>{bekliyor ? 'Giriş yapılıyor…' : 'Giriş yap'}</span>
              </Button>
            </form>
          </Card>
        </div>
      </div>
      <AltBilgi />
    </div>
  );
}
