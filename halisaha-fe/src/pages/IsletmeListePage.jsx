import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Power, PowerOff, Settings } from 'lucide-react';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Tag } from '@/components/ui/tag';
import { Table, Th, Td, Tr, Bos } from '@/components/ui/table';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import { ConfirmDialog } from '@/components/ui/dialog';
import { isletmeService } from '@/services';
import { apiHata } from '@/lib/api';
import { kisaTarih, telGoster } from '@/lib/format';
import { useHata, useToast } from '@/context/ToastContext';

export default function IsletmeListePage() {
  const navigate = useNavigate();
  const [isletmeler, setIsletmeler] = useState([]);
  const [hata, setHata] = useHata();
  const toast = useToast();
  const [yukleniyor, setYukleniyor] = useState(true);
  const [islemde, setIslemde] = useState(null);
  const [pasifOnay, setPasifOnay] = useState(null);

  const yukle = () => {
    setYukleniyor(true);
    setHata('');
    isletmeService
      .list()
      .then(setIsletmeler)
      .catch((e) => setHata(apiHata(e), { sessiz: true }))
      .finally(() => setYukleniyor(false));
  };

  useEffect(yukle, []);

  const durumDegistir = async (i) => {
    setIslemde(i.id);
    try {
      await isletmeService.durumGuncelle(i.id, i.durum === 'AKTIF' ? 'PASIF' : 'AKTIF');
      toast.basari('İşletme durumu güncellendi.');
      yukle();
    } catch (e) {
      setHata(apiHata(e));
    } finally {
      setIslemde(null);
    }
  };

  return (
    <div>
      <PageHeader kicker="Platform" baslik="İşletmeler" sade />
      <Hata mesaj={hata} onTekrar={yukle} />

      <Card className="gap-3.5">
        <div className="flex flex-wrap items-center gap-2.5">
          <h4 className="text-[22px]">Kayıtlı işletmeler</h4>
          <Button variant="primary" className="ml-auto" onClick={() => navigate('/isletmeler/yeni')}>
            <Plus size={16} strokeWidth={1.5} />
            <span>Yeni işletme</span>
          </Button>
        </div>

        {yukleniyor ? (
          <Yukleniyor />
        ) : (
          <Table kart>
            <thead>
              <tr>
                <Th>İşletme</Th>
                <Th>Yönetici</Th>
                <Th>Kayıt tarihi</Th>
                <Th>Durum</Th>
                <Th />
              </tr>
            </thead>
            <tbody>
              {isletmeler.length === 0 ? (
                <Bos colSpan={5}>İşletme kaydı bulunamadı.</Bos>
              ) : (
                isletmeler.map((i) => (
                  <Tr key={i.id}>
                    <Td tam className="font-semibold sm:font-normal">{i.ad}</Td>
                    <Td etiket="Yönetici" className="whitespace-nowrap">
                      {i.yoneticiAdSoyad || '—'}
                      {i.yoneticiTelNo ? <span className="ml-2 text-ink/55">{telGoster(i.yoneticiTelNo)}</span> : null}
                    </Td>
                    <Td etiket="Kayıt tarihi" className="whitespace-nowrap">{kisaTarih(String(i.kayitTarih).slice(0, 10))}</Td>
                    <Td etiket="Durum">
                      <Tag tone={i.durum === 'AKTIF' ? 'accent' : 'outline'}>
                        {i.durum === 'AKTIF' ? 'Aktif' : 'Pasif'}
                      </Tag>
                    </Td>
                    <Td tam className="whitespace-nowrap text-right">
                      <Button variant="cerceve" size="sm" onClick={() => navigate('/isletmeler/' + i.id)}>
                        <Settings size={14} strokeWidth={1.5} />
                        <span>Yönet</span>
                      </Button>
                      <Button
                        variant={i.durum === 'AKTIF' ? 'cerceveHata' : 'cerceve'}
                        size="sm"
                        className="ml-1"
                        disabled={islemde === i.id}
                        onClick={() => (i.durum === 'AKTIF' ? setPasifOnay(i) : durumDegistir(i))}
                      >
                        {i.durum === 'AKTIF' ? <PowerOff size={14} strokeWidth={1.5} /> : <Power size={14} strokeWidth={1.5} />}
                        <span>{i.durum === 'AKTIF' ? 'Pasife al' : 'Aktif et'}</span>
                      </Button>
                    </Td>
                  </Tr>
                ))
              )}
            </tbody>
          </Table>
        )}
      </Card>

      <ConfirmDialog
        open={!!pasifOnay}
        title="İşletme pasife alınsın mı?"
        description="Bu işletmenin tüm kullanıcılarının girişi engellenir ve açık oturumları kullanılamaz hale gelir. Veriler silinmez."
        isim={pasifOnay?.ad}
        confirmLabel="Pasife al"
        onConfirm={() => {
          const i = pasifOnay;
          setPasifOnay(null);
          durumDegistir(i);
        }}
        onCancel={() => setPasifOnay(null)}
      />
    </div>
  );
}
