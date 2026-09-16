import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Field, Input } from '@/components/ui/input';
import { Seg, SegOpt } from '@/components/ui/seg';
import { Hata } from '@/components/ui/durum';
import { kullaniciService } from '@/services';
import { apiHata } from '@/lib/api';
import { MODUL_LABEL, TUM_MODULLER } from '@/lib/yetki';

export default function PersonelKayitPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ adSoyad: '', kullaniciAdi: '', sifre: '', rol: 'PERSONEL' });
  const [yetkiler, setYetkiler] = useState([]);
  const [hata, setHata] = useState('');
  const [kaydediyor, setKaydediyor] = useState(false);

  const set = (k, v) => setForm((p) => ({ ...p, [k]: v }));

  const yetkiSecTogla = (modul) =>
    setYetkiler((p) => (p.includes(modul) ? p.filter((m) => m !== modul) : [...p, modul]));

  const kaydet = async (e) => {
    e.preventDefault();
    setHata('');
    setKaydediyor(true);
    try {
      await kullaniciService.create({
        adSoyad: form.adSoyad.trim(),
        kullaniciAdi: form.kullaniciAdi.trim(),
        sifre: form.sifre,
        rol: form.rol,
        yetkiler: form.rol === 'PERSONEL' ? yetkiler : [],
      });
      navigate('/personel', { replace: true });
    } catch (err) {
      setHata(apiHata(err));
    } finally {
      setKaydediyor(false);
    }
  };

  return (
    <div>
      <PageHeader kicker="Yönetim" baslik="Yeni personel" />
      <Hata mesaj={hata} />

      <form onSubmit={kaydet} className="max-w-[560px]">
        <Card className="gap-3.5">
          <h4 className="text-[22px]">Kullanıcı bilgileri</h4>

          <Field label="Ad soyad">
            <Input
              type="text"
              placeholder="Örn. Ayşe Yılmaz"
              value={form.adSoyad}
              onChange={(e) => set('adSoyad', e.target.value)}
              required
            />
          </Field>

          <div className="grid grid-cols-2 gap-3">
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

          <Field label="Rol">
            <Seg className="w-full">
              <SegOpt active={form.rol === 'PERSONEL'} onClick={() => set('rol', 'PERSONEL')} className="flex-1">
                Personel
              </SegOpt>
              <SegOpt active={form.rol === 'ADMIN'} onClick={() => set('rol', 'ADMIN')} className="flex-1">
                Admin
              </SegOpt>
            </Seg>
          </Field>

          {form.rol === 'PERSONEL' ? (
            <Field label="Başlangıç menü yetkileri">
              <p className="m-0 mb-1.5 text-[13px] text-ink/55">
                Personel varsayılan olarak yalnızca rezervasyon alabilir. İhtiyaç oldukça buradan veya
                personel listesinden diğer menüleri açabilirsiniz.
              </p>
              <div className="flex flex-wrap gap-2">
                {TUM_MODULLER.map((m) => (
                  <SegOpt
                    key={m}
                    active={yetkiler.includes(m)}
                    onClick={() => yetkiSecTogla(m)}
                    className="border border-divider"
                  >
                    {MODUL_LABEL[m]}
                  </SegOpt>
                ))}
              </div>
            </Field>
          ) : (
            <p className="m-0 text-[13.5px] text-ink/55">Admin kullanıcılar tüm menülere otomatik erişir.</p>
          )}

          <div className="mt-1 flex gap-2">
            <Button type="submit" variant="primary" className="flex-1" disabled={kaydediyor}>
              {kaydediyor ? 'Kaydediliyor…' : 'Kullanıcıyı kaydet'}
            </Button>
            <Button variant="secondary" onClick={() => navigate('/personel')}>
              Vazgeç
            </Button>
          </div>
        </Card>
      </form>
    </div>
  );
}
