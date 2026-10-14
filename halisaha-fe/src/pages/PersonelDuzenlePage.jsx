import { useEffect, useState } from 'react';
import { ArrowLeft, CircleCheck, CirclePause, Save, ShieldCheck, User } from 'lucide-react';
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
import GrupYetkiSecici from '@/components/GrupYetkiSecici';
import OgrenciIslemYetkileri from '@/components/OgrenciIslemYetkileri';
import { grupYetkisiGecerli, MODUL_LABEL, ModulIkon, OGRENCI_ISLEM_YETKILERI, TUM_MODULLER } from '@/lib/yetki';

export default function PersonelDuzenlePage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { kullanici: ben } = useAuth();
  const [kullanici, setKullanici] = useState(null);
  const [yetkiler, setYetkiler] = useState([]);
  const [grupYetki, setGrupYetki] = useState({ kisitli: false, grupIdleri: [] });
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
        setGrupYetki({ kisitli: k.grupKisitli, grupIdleri: k.grupIdleri });
        setBilgi({ kullaniciAdi: k.kullaniciAdi, adSoyad: k.adSoyad, sifre: '' });
      })
      .catch((e) => setHata(apiHata(e), { sessiz: true }))
      .finally(() => setYukleniyor(false));
  }, [id]);

  const yetkiSecTogla = (modul) =>
    setYetkiler((p) => {
      const yeni = p.includes(modul) ? p.filter((m) => m !== modul) : [...p, modul];
      // Öğrenciler menüsü kapanınca öğrenci işlem yetkileri de düşer
      return yeni.includes('OGRENCI') ? yeni : yeni.filter((m) => !OGRENCI_ISLEM_YETKILERI.includes(m));
    });

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

  // Menü ve grup yetkisi tek kartta; tek tuşla ikisi de kaydedilir
  const yetkileriKaydet = async () => {
    setHata('');
    const grupGorunur = grupYetkisiGecerli(yetkiler);
    if (grupGorunur && grupYetki.kisitli && grupYetki.grupIdleri.length === 0) {
      setHata('Seçili gruplar seçildiyse en az bir grup işaretleyin.');
      return;
    }
    setKaydediyor(true);
    try {
      let guncel = await kullaniciService.yetkileriGuncelle(id, yetkiler);
      // Gruba bağlı hiçbir menü açık değilse grup yetkisi alanı gizlidir; kısıt kaldırılır
      guncel = await kullaniciService.grupYetkisiniGuncelle(id, {
        grupKisitli: grupGorunur && grupYetki.kisitli,
        grupIdleri: grupGorunur && grupYetki.kisitli ? grupYetki.grupIdleri : [],
      });
      setKullanici(guncel);
      setYetkiler(guncel.yetkiler);
      setGrupYetki({ kisitli: guncel.grupKisitli, grupIdleri: guncel.grupIdleri });
      toast.basari('Yetkiler güncellendi.');
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

      <div className="grid max-w-[1040px] items-start gap-5 lg:grid-cols-2">
        <div className="flex flex-col gap-5">
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
                <Save size={16} strokeWidth={1.5} />
                <span>{kaydediyor ? 'Kaydediliyor…' : 'Bilgileri kaydet'}</span>
              </Button>
              <Button variant="secondary" onClick={() => navigate('/personel')}>
                <ArrowLeft size={16} strokeWidth={1.5} />
                <span>Listeye dön</span>
              </Button>
            </div>
          </Card>
        </form>

          <Card className="gap-4">
            <h4 className="text-[22px]">Erişim</h4>

            <div className="grid gap-2">
              <div className="font-heading text-[16px] font-semibold">Rol</div>
              <Seg className="w-full">
                <SegOpt
                  active={kullanici.rol === 'PERSONEL'}
                  tone="notr"
                  onClick={() => !kendisi && rolDegistir('PERSONEL')}
                  className="flex-1"
                >
                  <User size={16} strokeWidth={1.5} />
                  <span>Personel</span>
                </SegOpt>
                <SegOpt
                  active={kullanici.rol === 'ADMIN'}
                  onClick={() => !kendisi && rolDegistir('ADMIN')}
                  className="flex-1"
                >
                  <ShieldCheck size={16} strokeWidth={1.5} />
                  <span>Admin</span>
                </SegOpt>
              </Seg>
              <p className="m-0 text-[13.5px] text-ink/55">
                {kendisi
                  ? 'Kendi rolünüzü değiştiremezsiniz.'
                  : 'Admin tüm menülere erişir. Rol değişince menü yetkileri sıfırlanır ve kullanıcı yeniden giriş yapar.'}
              </p>
            </div>

            <div className="h-px bg-divider" />

            <div className="grid gap-2">
              <div className="font-heading text-[16px] font-semibold">Hesap durumu</div>
              <Seg className="w-full">
                <SegOpt active={kullanici.durum === 'AKTIF'} tone="basari" onClick={() => durumDegistir('AKTIF')} className="flex-1">
                  <CircleCheck size={16} strokeWidth={1.5} />
                  <span>Aktif</span>
                </SegOpt>
                <SegOpt active={kullanici.durum === 'PASIF'} tone="notr" onClick={() => durumDegistir('PASIF')} className="flex-1">
                  <CirclePause size={16} strokeWidth={1.5} />
                  <span>Pasif</span>
                </SegOpt>
              </Seg>
              <p className="m-0 text-[13.5px] text-ink/55">Pasif kullanıcılar sisteme giriş yapamaz.</p>
            </div>
          </Card>
        </div>

        {kullanici.rol === 'PERSONEL' ? (
          <Card className="gap-4">
            <h4 className="text-[22px]">Yetkiler</h4>

            <div className="grid gap-2">
              <div className="font-heading text-[16px] font-semibold">Menü yetkileri</div>
              <p className="m-0 text-[13.5px] text-ink/55">
                Varsayılan olarak yalnızca rezervasyon alabilir. Ek menüleri açıp kapatabilirsiniz.
              </p>
              <div className="flex flex-wrap gap-2">
                {TUM_MODULLER.map((m) => (
                  <SegOpt
                    key={m}
                    active={yetkiler.includes(m)}
                    onClick={() => yetkiSecTogla(m)}
                    className="border border-divider border-l!"
                  >
                    <ModulIkon modul={m} />
                    <span>{MODUL_LABEL[m]}</span>
                  </SegOpt>
                ))}
              </div>
            </div>

            {yetkiler.includes('OGRENCI') ? (
              <>
                <div className="h-px bg-divider" />
                <div className="grid gap-2">
                  <div className="font-heading text-[16px] font-semibold">Öğrenci işlem yetkileri</div>
                  <p className="m-0 text-[13.5px] text-ink/55">
                    Öğrenci detayında hangi kartları görüp işlem yapabileceği. İşaretlenmeyen kart hiç görünmez.
                  </p>
                  <OgrenciIslemYetkileri yetkiler={yetkiler} onTogla={yetkiSecTogla} />
                </div>
              </>
            ) : null}

            {grupYetkisiGecerli(yetkiler) ? (
              <>
            <div className="h-px bg-divider" />

            <div className="grid gap-2">
              <div className="font-heading text-[16px] font-semibold">Grup yetkisi</div>
              <p className="m-0 text-[13.5px] text-ink/55">
                Belirli gruplar seçerseniz öğrenciler, yoklama ve transfer ekranlarında yalnızca o grupların öğrencileri görünür.
              </p>
              <GrupYetkiSecici value={grupYetki} onChange={setGrupYetki} />
            </div>
              </>
            ) : null}

            <Button type="button" variant="primary" disabled={kaydediyor} onClick={yetkileriKaydet}>
              <Save size={16} strokeWidth={1.5} />
              <span>{kaydediyor ? 'Kaydediliyor…' : 'Yetkileri kaydet'}</span>
            </Button>
          </Card>
        ) : null}
      </div>
    </div>
  );
}
