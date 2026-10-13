import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Field, Input } from '@/components/ui/input';
import { Seg, SegOpt } from '@/components/ui/seg';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import { kullaniciService } from '@/services';
import { apiHata } from '@/lib/api';
import { useAuth } from '@/context/AuthContext';
import { useHata, useToast } from '@/context/ToastContext';
import { MODUL_LABEL, TUM_MODULLER } from '@/lib/yetki';

export default function PersonelDuzenlePage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { kullanici: ben } = useAuth();
  const [kullanici, setKullanici] = useState(null);
  const [yetkiler, setYetkiler] = useState([]);
  const [bilgi, setBilgi] = useState({ kullaniciAdi: '', adSoyad: '', sifre: '' });
  const [hata, setHata] = useHata();
  const toast = useToast();
  const [yukleniyor, setYukleniyor] = useState(true);
  const [kaydediyor, setKaydediyor] = useState(false);

  useEffect(() => {
    kullaniciService
      .get(id)
      .then((k) => {
        setKullanici(k);
        setYetkiler(k.yetkiler);
        setBilgi({ kullaniciAdi: k.kullaniciAdi, adSoyad: k.adSoyad, sifre: '' });
      })
      .catch((e) => setHata(apiHata(e), { sessiz: true }))
      .finally(() => setYukleniyor(false));
  }, [id]);

  const yetkiSecTogla = (modul) =>
    setYetkiler((p) => (p.includes(modul) ? p.filter((m) => m !== modul) : [...p, modul]));

  const bilgiKaydet = async (e) => {
    e.preventDefault();
    setHata('');
    setKaydediyor(true);
    try {
      const kullaniciAdiDegisti = bilgi.kullaniciAdi.trim() !== kullanici.kullaniciAdi;
      const guncel = await kullaniciService.guncelle(id, {
        kullaniciAdi: bilgi.kullaniciAdi.trim(),
        adSoyad: bilgi.adSoyad.trim(),
        ...(bilgi.sifre ? { sifre: bilgi.sifre } : {}),
      });
      setKullanici(guncel);
      setBilgi({ kullaniciAdi: guncel.kullaniciAdi, adSoyad: guncel.adSoyad, sifre: '' });
      toast.basari(
        kullaniciAdiDegisti || bilgi.sifre
          ? 'Bilgiler güncellendi; kullanıcının yeniden giriş yapması gerekecek.'
          : 'Bilgiler güncellendi.',
      );
    } catch (err) {
      setHata(apiHata(err));
    } finally {
      setKaydediyor(false);
    }
  };

  const yetkileriKaydet = async () => {
    setHata('');
    setKaydediyor(true);
    try {
      const guncel = await kullaniciService.yetkileriGuncelle(id, yetkiler);
      setKullanici(guncel);
      setYetkiler(guncel.yetkiler);
      toast.basari('Menü yetkileri güncellendi.');
    } catch (err) {
      setHata(apiHata(err));
    } finally {
      setKaydediyor(false);
    }
  };

  const rolDegistir = async (rol) => {
    if (rol === kullanici.rol) return;
    setHata('');
    setKaydediyor(true);
    try {
      const guncel = await kullaniciService.rolGuncelle(id, rol);
      setKullanici(guncel);
      setYetkiler(guncel.yetkiler);
      toast.basari('Rol güncellendi; kullanıcının yeniden giriş yapması gerekecek.');
    } catch (err) {
      setHata(apiHata(err));
    } finally {
      setKaydediyor(false);
    }
  };

  const durumDegistir = async (durum) => {
    setHata('');
    setKaydediyor(true);
    try {
      const guncel = await kullaniciService.durumGuncelle(id, durum);
      setKullanici(guncel);
      toast.basari('Personel durumu güncellendi.');
    } catch (err) {
      setHata(apiHata(err));
    } finally {
      setKaydediyor(false);
    }
  };

  if (yukleniyor) return <Yukleniyor />;
  const kendisi = ben?.id === kullanici?.id;

  if (!kullanici) return <Hata mesaj={hata || 'Kullanıcı bulunamadı.'} />;

  return (
    <div>
      <PageHeader kicker="Yönetim" baslik={kullanici.adSoyad} />
      <Hata mesaj={hata} />

      <div className="grid items-start gap-5 [grid-template-columns:repeat(auto-fit,minmax(min(300px,100%),1fr))]">
        <form onSubmit={bilgiKaydet}>
          <Card className="gap-3.5">
            <h4 className="text-[22px]">Kullanıcı bilgileri</h4>
            <Field label="Kullanıcı adı">
              <Input
                type="text"
                autoComplete="off"
                value={bilgi.kullaniciAdi}
                onChange={(e) => setBilgi((p) => ({ ...p, kullaniciAdi: e.target.value }))}
                required
              />
            </Field>
            <Field label="Ad soyad">
              <Input
                type="text"
                value={bilgi.adSoyad}
                onChange={(e) => setBilgi((p) => ({ ...p, adSoyad: e.target.value }))}
                required
              />
            </Field>
            <Field label="Yeni şifre">
              <Input
                type="password"
                autoComplete="new-password"
                minLength={6}
                placeholder="Değiştirmek istemiyorsanız boş bırakın"
                value={bilgi.sifre}
                onChange={(e) => setBilgi((p) => ({ ...p, sifre: e.target.value }))}
              />
            </Field>
            <div className="mt-1 flex gap-2">
              <Button type="submit" variant="primary" className="flex-1" disabled={kaydediyor || !bilgi.adSoyad.trim() || !bilgi.kullaniciAdi.trim()}>
                {kaydediyor ? 'Kaydediliyor…' : 'Bilgileri kaydet'}
              </Button>
              <Button variant="secondary" onClick={() => navigate('/personel')}>
                Listeye dön
              </Button>
            </div>
          </Card>
        </form>

        <Card className="gap-3.5">
          <h4 className="text-[22px]">Rol</h4>
          <Seg className="w-full">
            <SegOpt
              active={kullanici.rol === 'PERSONEL'}
              onClick={() => !kendisi && rolDegistir('PERSONEL')}
              className="flex-1"
            >
              Personel
            </SegOpt>
            <SegOpt
              active={kullanici.rol === 'ADMIN'}
              onClick={() => !kendisi && rolDegistir('ADMIN')}
              className="flex-1"
            >
              Admin
            </SegOpt>
          </Seg>
          <p className="m-0 text-[13.5px] text-ink/55">
            {kendisi
              ? 'Kendi rolünüzü değiştiremezsiniz.'
              : 'Admin tüm menülere erişir. Rol değişince menü yetkileri sıfırlanır ve kullanıcı yeniden giriş yapar.'}
          </p>
        </Card>

        {kullanici.rol === 'PERSONEL' ? (
        <Card className="gap-3.5">
          <h4 className="text-[22px]">Menü yetkileri</h4>
          <p className="m-0 text-[13.5px] text-ink/55">
            {kullanici.kullaniciAdi} · varsayılan olarak yalnızca rezervasyon alabilir. Aşağıdan ek menüleri
            açıp kapatabilirsiniz.
          </p>

          <div className="flex flex-wrap gap-2">
            {TUM_MODULLER.map((m) => (
              <SegOpt
                key={m}
                active={yetkiler.includes(m)}
                onClick={() => yetkiSecTogla(m)}
                className="border border-divider border-l!"
              >
                {MODUL_LABEL[m]}
              </SegOpt>
            ))}
          </div>

          <div className="mt-1 flex gap-2">
            <Button type="button" variant="primary" className="flex-1" disabled={kaydediyor} onClick={yetkileriKaydet}>
              {kaydediyor ? 'Kaydediliyor…' : 'Yetkileri kaydet'}
            </Button>
          </div>
        </Card>
        ) : null}

        <Card className="gap-3.5">
          <h4 className="text-[22px]">Hesap durumu</h4>
          <Seg className="w-full">
            <SegOpt active={kullanici.durum === 'AKTIF'} onClick={() => durumDegistir('AKTIF')} className="flex-1">
              Aktif
            </SegOpt>
            <SegOpt active={kullanici.durum === 'PASIF'} onClick={() => durumDegistir('PASIF')} className="flex-1">
              Pasif
            </SegOpt>
          </Seg>
          <p className="m-0 text-[13.5px] text-ink/55">
            Pasif kullanıcılar sisteme giriş yapamaz.
          </p>
        </Card>
      </div>
    </div>
  );
}
