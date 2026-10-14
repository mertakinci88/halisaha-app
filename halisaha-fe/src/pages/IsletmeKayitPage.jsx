import { useState } from 'react';
import { Save, X } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Field, Input, TelInput } from '@/components/ui/input';
import { Hata } from '@/components/ui/durum';
import { isletmeService } from '@/services';
import { apiHata } from '@/lib/api';

import { useHata, useToast } from '@/context/ToastContext';

export default function IsletmeKayitPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ ad: '', yoneticiAdSoyad: '', yoneticiTelNo: '', adSoyad: '', kullaniciAdi: '', sifre: '' });
  const [hata, setHata] = useHata();
  const toast = useToast();
  const [kaydediyor, setKaydediyor] = useState(false);

  const set = (k, v) => setForm((p) => ({ ...p, [k]: v }));

  const kaydet = async (e) => {
    e.preventDefault();
    setHata('');
    setKaydediyor(true);
    try {
      const isletme = await isletmeService.create({
        ad: form.ad.trim(),
        yoneticiAdSoyad: form.yoneticiAdSoyad.trim(),
        yoneticiTelNo: form.yoneticiTelNo ? '0' + form.yoneticiTelNo : '',
        yonetici: {
          adSoyad: form.adSoyad.trim(),
          kullaniciAdi: form.kullaniciAdi.trim(),
          sifre: form.sifre,
        },
      });
      toast.basari('İşletme ve yöneticisi oluşturuldu.');
      navigate('/isletmeler/' + isletme.id, { replace: true });
    } catch (err) {
      setHata(apiHata(err));
    } finally {
      setKaydediyor(false);
    }
  };

  return (
    <div>
      <PageHeader kicker="Platform" baslik="Yeni işletme" sade />
      <Hata mesaj={hata} />

      <form onSubmit={kaydet} className="max-w-[560px]">
        <Card className="gap-3.5">
          <h4 className="text-[22px]">İşletme</h4>
          <Field label="İşletme adı">
            <Input
              type="text"
              placeholder="Örn. Yıldız Halı Saha"
              value={form.ad}
              onChange={(e) => set('ad', e.target.value)}
              required
            />
          </Field>

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <Field label="Yönetici ad soyad">
              <Input
                type="text"
                placeholder="Örn. Halil Bey"
                value={form.yoneticiAdSoyad}
                onChange={(e) => set('yoneticiAdSoyad', e.target.value)}
              />
            </Field>
            <Field label="Yönetici telefon">
              <TelInput value={form.yoneticiTelNo} onChange={(v) => set('yoneticiTelNo', v)} />
            </Field>
          </div>

          <h4 className="mt-2 text-[22px]">Yönetici hesabı</h4>
          <p className="m-0 -mt-2 text-[13px] text-ink/55">
            İşletmenin admin giriş hesabıdır; sahaları, personeli ve diğer tüm verileri bu hesap yönetir.
          </p>
          <Field label="Ad soyad">
            <Input
              type="text"
              placeholder="Örn. Ayşe Yılmaz"
              value={form.adSoyad}
              onChange={(e) => set('adSoyad', e.target.value)}
              required
            />
          </Field>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <Field label="Kullanıcı adı">
              <Input
                type="text"
                autoComplete="off"
                placeholder="ayse.yilmaz"
                value={form.kullaniciAdi}
                onChange={(e) => set('kullaniciAdi', e.target.value)}
                required
              />
            </Field>
            <Field label="Şifre">
              <Input
                type="password"
                autoComplete="new-password"
                minLength={6}
                value={form.sifre}
                onChange={(e) => set('sifre', e.target.value)}
                required
              />
            </Field>
          </div>

          <div className="mt-1 flex gap-2">
            <Button type="submit" variant="primary" className="flex-1" disabled={kaydediyor}>
              <Save size={16} strokeWidth={1.5} />
              <span>{kaydediyor ? 'Kaydediliyor…' : 'İşletmeyi oluştur'}</span>
            </Button>
            <Button variant="cerceveHata" onClick={() => navigate('/isletmeler')}>
              <X size={16} strokeWidth={1.5} />
              <span>Vazgeç</span>
            </Button>
          </div>
        </Card>
      </form>
    </div>
  );
}
