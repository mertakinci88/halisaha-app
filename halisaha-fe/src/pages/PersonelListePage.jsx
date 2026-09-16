import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, ShieldCheck } from 'lucide-react';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Tag } from '@/components/ui/tag';
import { Table, Th, Td, Tr, Bos } from '@/components/ui/table';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import { kullaniciService } from '@/services';
import { apiHata } from '@/lib/api';
import { MODUL_LABEL } from '@/lib/yetki';

export default function PersonelListePage() {
  const navigate = useNavigate();
  const [kullanicilar, setKullanicilar] = useState([]);
  const [hata, setHata] = useState('');
  const [yukleniyor, setYukleniyor] = useState(true);
  const [islemde, setIslemde] = useState(null);

  const yukle = () => {
    setYukleniyor(true);
    setHata('');
    kullaniciService
      .list()
      .then(setKullanicilar)
      .catch((e) => setHata(apiHata(e)))
      .finally(() => setYukleniyor(false));
  };

  useEffect(yukle, []);

  const durumDegistir = async (k) => {
    setIslemde(k.id);
    try {
      const yeniDurum = k.durum === 'AKTIF' ? 'PASIF' : 'AKTIF';
      await kullaniciService.durumGuncelle(k.id, yeniDurum);
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
          <Table>
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
                    <Td>{k.adSoyad}</Td>
                    <Td>{k.kullaniciAdi}</Td>
                    <Td>
                      {k.rol === 'ADMIN' ? (
                        <Tag tone="accent">
                          <ShieldCheck size={13} strokeWidth={1.75} className="mr-1 inline" />
                          Admin
                        </Tag>
                      ) : (
                        <Tag tone="neutral">Personel</Tag>
                      )}
                    </Td>
                    <Td>
                      {k.rol === 'ADMIN' ? (
                        <span className="text-ink/55">Tüm menüler</span>
                      ) : k.yetkiler.length === 0 ? (
                        <span className="text-ink/55">Sadece rezervasyon</span>
                      ) : (
                        <div className="flex flex-wrap gap-1">
                          {k.yetkiler.map((m) => (
                            <Tag key={m} tone="outline">
                              {MODUL_LABEL[m] || m}
                            </Tag>
                          ))}
                        </div>
                      )}
                    </Td>
                    <Td>
                      <Tag tone={k.durum === 'AKTIF' ? 'accent' : 'outline'}>
                        {k.durum === 'AKTIF' ? 'Aktif' : 'Pasif'}
                      </Tag>
                    </Td>
                    <Td className="whitespace-nowrap text-right">
                      {k.rol !== 'ADMIN' ? (
                        <Button variant="secondary" size="sm" onClick={() => navigate('/personel/' + k.id)}>
                          Yetkileri düzenle
                        </Button>
                      ) : null}
                      <Button
                        variant="ghost"
                        size="sm"
                        className="ml-1"
                        disabled={islemde === k.id}
                        onClick={() => durumDegistir(k)}
                      >
                        {k.durum === 'AKTIF' ? 'Pasife al' : 'Aktif et'}
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
