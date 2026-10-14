import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Pencil, Plus, ShieldCheck, User, UserCheck, UserX } from 'lucide-react';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Tag } from '@/components/ui/tag';
import { Table, Th, Td, Tr, Bos } from '@/components/ui/table';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import { kullaniciService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata, useToast } from '@/context/ToastContext';
import { MODUL_LABEL, TUM_MODULLER } from '@/lib/yetki';

export default function PersonelListePage() {
  const navigate = useNavigate();
  const [kullanicilar, setKullanicilar] = useState([]);
  const [hata, setHata] = useHata();
  const toast = useToast();
  const [yukleniyor, setYukleniyor] = useState(true);
  const [islemde, setIslemde] = useState(null);

  const yukle = () => {
    setYukleniyor(true);
    setHata('');
    kullaniciService
      .list()
      .then(setKullanicilar)
      .catch((e) => setHata(apiHata(e), { sessiz: true }))
      .finally(() => setYukleniyor(false));
  };

  useEffect(yukle, []);

  const durumDegistir = async (k) => {
    setIslemde(k.id);
    try {
      const yeniDurum = k.durum === 'AKTIF' ? 'PASIF' : 'AKTIF';
      await kullaniciService.durumGuncelle(k.id, yeniDurum);
      toast.basari('Personel durumu güncellendi.');
      yukle();
    } catch (e) {
      setHata(apiHata(e));
    } finally {
      setIslemde(null);
    }
  };

  return (
    <div>
      <PageHeader kicker="Yönetim" baslik="Personel" />
      <Hata mesaj={hata} onTekrar={yukle} />

      <Card className="gap-3.5">
        <div className="flex flex-wrap items-center gap-2.5">
          <h4 className="text-[22px]">Kullanıcılar</h4>
          <Button variant="primary" className="ml-auto" onClick={() => navigate('/personel/yeni')}>
            <Plus size={16} strokeWidth={1.5} />
            <span>Yeni personel</span>
          </Button>
        </div>

        {yukleniyor ? (
          <Yukleniyor />
        ) : (
          <Table kart>
            <thead>
              <tr>
                <Th>Ad soyad</Th>
                <Th>Kullanıcı adı</Th>
                <Th>Rol</Th>
                <Th>Menü yetkileri</Th>
                <Th>Durum</Th>
                <Th />
              </tr>
            </thead>
            <tbody>
              {kullanicilar.length === 0 ? (
                <Bos colSpan={6}>Kullanıcı kaydı bulunamadı.</Bos>
              ) : (
                kullanicilar.map((k) => (
                  <Tr key={k.id}>
                    <Td tam className="font-semibold sm:font-normal">{k.adSoyad}</Td>
                    <Td etiket="Kullanıcı adı">{k.kullaniciAdi}</Td>
                    <Td etiket="Rol">
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
                    <Td tam etiket="Menü yetkileri">
                      {k.rol === 'ADMIN' ? (
                        <span className="text-ink/55">Tüm menüler</span>
                      ) : k.yetkiler.filter((m) => TUM_MODULLER.includes(m)).length === 0 ? (
                        <span className="text-ink/55">Sadece rezervasyon</span>
                      ) : (
                        <div className="flex flex-wrap gap-1">
                          {k.yetkiler.filter((m) => TUM_MODULLER.includes(m)).map((m) => (
                            <Tag key={m} tone="outline">
                              {MODUL_LABEL[m] || m}
                            </Tag>
                          ))}
                        </div>
                      )}
                    </Td>
                    <Td etiket="Durum">
                      <Tag tone={k.durum === 'AKTIF' ? 'accent' : 'outline'}>
                        {k.durum === 'AKTIF' ? 'Aktif' : 'Pasif'}
                      </Tag>
                    </Td>
                    <Td tam className="whitespace-nowrap text-right">
                      <Button variant="cerceve" size="sm" onClick={() => navigate('/personel/' + k.id)}>
                        <Pencil size={14} strokeWidth={1.5} />
                        <span>Düzenle</span>
                      </Button>
                      <Button
                        variant={k.durum === 'AKTIF' ? 'cerceveHata' : 'cerceve'}
                        size="sm"
                        className="ml-1"
                        disabled={islemde === k.id}
                        onClick={() => durumDegistir(k)}
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
        )}
      </Card>
    </div>
  );
}
