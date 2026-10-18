import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, KeyRound, Plus, Save, ShieldCheck, User, UserCheck, UserX, X } from 'lucide-react';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Field, Input, TelInput } from '@/components/ui/input';
import { Seg, SegOpt } from '@/components/ui/seg';
import { Tag } from '@/components/ui/tag';
import { Table, Th, Td, Tr, Bos } from '@/components/ui/table';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import { isletmeService } from '@/services';
import { apiHata } from '@/lib/api';
import { telRakam } from '@/lib/format';
import { useHata, useToast } from '@/context/ToastContext';

const BOS_KULLANICI = { adSoyad: '', kullaniciAdi: '', sifre: '', rol: 'ADMIN' };

export default function IsletmeDetayPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [isletme, setIsletme] = useState(null);
  const [bilgi, setBilgi] = useState({ ad: '', yoneticiAdSoyad: '', yoneticiTelNo: '' });
  const [kullanicilar, setKullanicilar] = useState([]);
  const [yeni, setYeni] = useState(BOS_KULLANICI);
  const [sifreHedef, setSifreHedef] = useState(null);
  const [yeniSifre, setYeniSifre] = useState('');
  const [hata, setHata] = useHata();
  const toast = useToast();
  const [yukleniyor, setYukleniyor] = useState(true);
  const [islemde, setIslemde] = useState(false);

  const yukle = () =>
    Promise.all([isletmeService.get(id), isletmeService.kullanicilar(id)])
      .then(([i, k]) => {
        setIsletme(i);
        setBilgi({
          ad: i.ad,
          yoneticiAdSoyad: i.yoneticiAdSoyad || '',
          yoneticiTelNo: telRakam(i.yoneticiTelNo),
        });
        setKullanicilar(k);
      })
      .catch((e) => setHata(apiHata(e), { sessiz: true }))
      .finally(() => setYukleniyor(false));

  useEffect(() => {
    yukle();
  }, [id]);

  const calistir = async (is, basariMesaji) => {
    setHata('');
    setIslemde(true);
    try {
      await is();
      toast.basari(basariMesaji);
      await yukle();
      return true;
    } catch (e) {
      setHata(apiHata(e));
      return false;
    } finally {
      setIslemde(false);
    }
  };

  const adKaydet = (e) => {
    e.preventDefault();
    calistir(
      () =>
        isletmeService.update(id, {
          ad: bilgi.ad.trim(),
          yoneticiAdSoyad: bilgi.yoneticiAdSoyad.trim(),
          yoneticiTelNo: bilgi.yoneticiTelNo ? '0' + bilgi.yoneticiTelNo : '',
        }),
      'İşletme güncellendi.',
    );
  };

  const kullaniciEkle = async (e) => {
    e.preventDefault();
    const ok = await calistir(
      () =>
        isletmeService.kullaniciOlustur(id, {
          adSoyad: yeni.adSoyad.trim(),
          kullaniciAdi: yeni.kullaniciAdi.trim(),
          sifre: yeni.sifre,
          rol: yeni.rol,
          yetkiler: [],
        }),
      'Kullanıcı oluşturuldu.',
    );
    if (ok) setYeni(BOS_KULLANICI);
  };

  const sifreKaydet = async (e) => {
    e.preventDefault();
    const ok = await calistir(
      () => isletmeService.sifreSifirla(id, sifreHedef.id, yeniSifre),
      'Şifre güncellendi.',
    );
    if (ok) {
      setSifreHedef(null);
      setYeniSifre('');
    }
  };

  if (yukleniyor) return <Yukleniyor />;
  if (!isletme) return <Hata mesaj={hata || 'İşletme bulunamadı.'} onTekrar={yukle} />;

  return (
    <div>
      <PageHeader kicker="Platform" baslik={isletme.ad} sade>
        <Button variant="secondary" onClick={() => navigate('/isletmeler')}>
          <ArrowLeft size={16} strokeWidth={1.5} />
          <span>Listeye dön</span>
        </Button>
      </PageHeader>
      <Hata mesaj={hata} />

      <div className="grid gap-5 lg:grid-cols-[minmax(0,420px)_minmax(0,1fr)]">
        <div className="flex min-w-0 flex-col gap-5">
          <form onSubmit={adKaydet}>
            <Card className="gap-3.5">
              <div className="flex items-center gap-2.5">
                <h4 className="text-[22px]">İşletme</h4>
                <Tag tone={isletme.durum === 'AKTIF' ? 'accent' : 'outline'} className="ml-auto">
                  {isletme.durum === 'AKTIF' ? 'Aktif' : 'Pasif'}
                </Tag>
              </div>
              <Field label="İşletme adı">
                <Input
                  type="text"
                  value={bilgi.ad}
                  onChange={(e) => setBilgi((p) => ({ ...p, ad: e.target.value }))}
                  required
                />
              </Field>
              <Field label="Yönetici ad soyad">
                <Input
                  type="text"
                  value={bilgi.yoneticiAdSoyad}
                  onChange={(e) => setBilgi((p) => ({ ...p, yoneticiAdSoyad: e.target.value }))}
                />
              </Field>
              <Field label="Yönetici telefon">
                <TelInput value={bilgi.yoneticiTelNo} onChange={(v) => setBilgi((p) => ({ ...p, yoneticiTelNo: v }))} />
              </Field>
              <Button type="submit" variant="primary" disabled={islemde || !bilgi.ad.trim()}>
                <Save size={16} strokeWidth={1.5} />
                <span>Bilgileri güncelle</span>
              </Button>
            </Card>
          </form>

          <form onSubmit={kullaniciEkle}>
            <Card className="gap-3.5">
              <h4 className="text-[22px]">Kullanıcı ekle</h4>
              <Field label="Ad soyad">
                <Input
                  type="text"
                  value={yeni.adSoyad}
                  onChange={(e) => setYeni((p) => ({ ...p, adSoyad: e.target.value }))}
                  required
                />
              </Field>
              <Field label="Kullanıcı adı">
                <Input
                  type="text"
                  autoComplete="off"
                  value={yeni.kullaniciAdi}
                  onChange={(e) => setYeni((p) => ({ ...p, kullaniciAdi: e.target.value }))}
                  required
                />
              </Field>
              <Field label="Şifre">
                <Input
                  type="password"
                  autoComplete="new-password"
                  minLength={6}
                  value={yeni.sifre}
                  onChange={(e) => setYeni((p) => ({ ...p, sifre: e.target.value }))}
                  required
                />
              </Field>
              <Field label="Rol">
                <Seg className="w-full">
                  <SegOpt
                    active={yeni.rol === 'ADMIN'}
                    onClick={() => setYeni((p) => ({ ...p, rol: 'ADMIN' }))}
                    className="flex-1"
                  >
                    <ShieldCheck size={16} strokeWidth={1.5} />
                    <span>Admin</span>
                  </SegOpt>
                  <SegOpt
                    active={yeni.rol === 'PERSONEL'}
                    tone="notr"
                    onClick={() => setYeni((p) => ({ ...p, rol: 'PERSONEL' }))}
                    className="flex-1"
                  >
                    <User size={16} strokeWidth={1.5} />
                    <span>Personel</span>
                  </SegOpt>
                </Seg>
              </Field>
              <Button type="submit" variant="primary" disabled={islemde}>
                <Plus size={16} strokeWidth={1.5} />
                <span>Kullanıcı ekle</span>
              </Button>
            </Card>
          </form>
        </div>

        <Card className="min-w-0 gap-3.5 self-start">
          <h4 className="text-[22px]">Kullanıcılar</h4>
          <Table>
            <thead>
              <tr>
                <Th>Ad soyad</Th>
                <Th>Kullanıcı adı</Th>
                <Th>Rol</Th>
                <Th>Durum</Th>
                <Th />
              </tr>
            </thead>
            <tbody>
              {kullanicilar.length === 0 ? (
                <Bos colSpan={5}>Kullanıcı bulunamadı.</Bos>
              ) : (
                kullanicilar.map((k) => (
                  <Tr key={k.id}>
                    <Td>{k.adSoyad}</Td>
                    <Td>{k.kullaniciAdi}</Td>
                    <Td>
                      {k.rol === 'ADMIN' ? (
                        <Tag tone="accent">
                          <ShieldCheck size={13} strokeWidth={1.75} className="mr-1 inline" />
                          Admin
                        </Tag>
                      ) : (
                        <Tag tone="neutral">
                          <User size={13} strokeWidth={1.75} className="mr-1 inline" />
                          Personel
                        </Tag>
                      )}
                    </Td>
                    <Td>
                      <Tag tone={k.durum === 'AKTIF' ? 'accent' : 'outline'}>
                        {k.durum === 'AKTIF' ? 'Aktif' : 'Pasif'}
                      </Tag>
                    </Td>
                    <Td className="whitespace-nowrap text-right">
                      <Button
                        variant="cerceve"
                        size="sm"
                        onClick={() => {
                          setSifreHedef(k);
                          setYeniSifre('');
                        }}
                      >
                        <KeyRound size={14} strokeWidth={1.5} />
                        <span>Şifre sıfırla</span>
                      </Button>
                      <Button
                        variant={k.durum === 'AKTIF' ? 'cerceveHata' : 'cerceve'}
                        size="sm"
                        className="ml-1"
                        disabled={islemde}
                        onClick={() =>
                          calistir(
                            () => isletmeService.kullaniciDurum(id, k.id, k.durum === 'AKTIF' ? 'PASIF' : 'AKTIF'),
                            'Kullanıcı durumu güncellendi.',
                          )
                        }
                      >
                        {k.durum === 'AKTIF' ? <UserX size={14} strokeWidth={1.5} /> : <UserCheck size={14} strokeWidth={1.5} />}
                        <span>{k.durum === 'AKTIF' ? 'Pasife al' : 'Aktif et'}</span>
                      </Button>
                    </Td>
                  </Tr>
                ))
              )}
            </tbody>
          </Table>

          {sifreHedef ? (
            <form onSubmit={sifreKaydet} className="flex flex-col gap-2.5 border border-divider p-3.5">
              <div className="font-heading text-[17px]">{sifreHedef.adSoyad} için yeni şifre</div>
              <Input
                type="password"
                autoComplete="new-password"
                minLength={6}
                placeholder="En az 6 karakter"
                value={yeniSifre}
                onChange={(e) => setYeniSifre(e.target.value)}
                required
              />
              <div className="flex gap-2">
                <Button type="submit" variant="primary" className="flex-1" disabled={islemde}>
                  <Save size={16} strokeWidth={1.5} />
                  <span>Şifreyi kaydet</span>
                </Button>
                <Button variant="cerceveHata" onClick={() => setSifreHedef(null)}>
                  <X size={16} strokeWidth={1.5} />
                  <span>Vazgeç</span>
                </Button>
              </div>
            </form>
          ) : null}
        </Card>
      </div>
    </div>
  );
}
