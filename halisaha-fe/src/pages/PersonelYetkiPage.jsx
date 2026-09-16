import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Seg, SegOpt } from '@/components/ui/seg';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import { kullaniciService } from '@/services';
import { apiHata } from '@/lib/api';
import { MODUL_LABEL, TUM_MODULLER } from '@/lib/yetki';

export default function PersonelYetkiPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [kullanici, setKullanici] = useState(null);
  const [yetkiler, setYetkiler] = useState([]);
  const [hata, setHata] = useState('');
  const [basari, setBasari] = useState('');
  const [yukleniyor, setYukleniyor] = useState(true);
  const [kaydediyor, setKaydediyor] = useState(false);

  useEffect(() => {
    kullaniciService
      .get(id)
      .then((k) => {
        setKullanici(k);
        setYetkiler(k.yetkiler);
      })
      .catch((e) => setHata(apiHata(e)))
      .finally(() => setYukleniyor(false));
  }, [id]);

  const yetkiSecTogla = (modul) =>
    setYetkiler((p) => (p.includes(modul) ? p.filter((m) => m !== modul) : [...p, modul]));

  const yetkileriKaydet = async () => {
    setHata('');
    setBasari('');
    setKaydediyor(true);
    try {
      const guncel = await kullaniciService.yetkileriGuncelle(id, yetkiler);
      setKullanici(guncel);
      setYetkiler(guncel.yetkiler);
      setBasari('Menü yetkileri güncellendi.');
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
    } catch (err) {
      setHata(apiHata(err));
    } finally {
      setKaydediyor(false);
    }
  };

  if (yukleniyor) return <Yukleniyor />;
  if (!kullanici) return <Hata mesaj={hata || 'Kullanıcı bulunamadı.'} />;

  return (
    <div>
      <PageHeader kicker="Yönetim" baslik={kullanici.adSoyad} />
      <Hata mesaj={hata} />
      {basari ? <p className="mb-4 text-[14.5px] text-accent-700">{basari}</p> : null}

      <div className="grid items-start gap-5 [grid-template-columns:repeat(auto-fit,minmax(300px,1fr))]">
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
                className="border border-divider"
              >
                {MODUL_LABEL[m]}
              </SegOpt>
            ))}
          </div>

          <div className="mt-1 flex gap-2">
            <Button type="button" variant="primary" className="flex-1" disabled={kaydediyor} onClick={yetkileriKaydet}>
              {kaydediyor ? 'Kaydediliyor…' : 'Yetkileri kaydet'}
            </Button>
            <Button variant="secondary" onClick={() => navigate('/personel')}>
              Listeye dön
            </Button>
          </div>
        </Card>

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
