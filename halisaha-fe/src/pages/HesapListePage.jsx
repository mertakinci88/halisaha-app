import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus } from 'lucide-react';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Tag } from '@/components/ui/tag';
import { Table, Th, Td, Tr, Bos } from '@/components/ui/table';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import { ConfirmDialog } from '@/components/ui/dialog';
import { hesapService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata, useToast } from '@/context/ToastContext';
import { ibanMaske } from '@/lib/format';

export default function HesapListePage() {
  const navigate = useNavigate();
  const [hesaplar, setHesaplar] = useState([]);
  const [hata, setHata] = useHata();
  const toast = useToast();
  const [yukleniyor, setYukleniyor] = useState(true);
  const [silinecek, setSilinecek] = useState(null);

  const yukle = () => {
    setYukleniyor(true);
    setHata('');
    hesapService
      .list()
      .then(setHesaplar)
      .catch((e) => setHata(apiHata(e), { sessiz: true }))
      .finally(() => setYukleniyor(false));
  };

  useEffect(yukle, []);

  const sil = async () => {
    const h = silinecek;
    setSilinecek(null);
    try {
      await hesapService.remove(h.id);
      toast.basari('Hesap silindi; kayıtlarda kullanılmışsa pasife alındı.');
      yukle();
    } catch (e) {
      setHata(apiHata(e));
    }
  };

  return (
    <div>
      <PageHeader kicker="Tanımlar" baslik="Hesaplar" />
      <Hata mesaj={hata} onTekrar={yukle} />

      <Card className="gap-3.5">
        <div className="flex flex-wrap items-center gap-2.5">
          <h4 className="text-[22px]">Havale / EFT hesapları</h4>
          <Button variant="primary" className="ml-auto" onClick={() => navigate('/hesaplar/yeni')}>
            <Plus size={16} strokeWidth={1.5} />
            <span>Yeni hesap</span>
          </Button>
        </div>

        {yukleniyor ? (
          <Yukleniyor />
        ) : (
          <Table kart>
            <thead>
              <tr>
                <Th>Hesap adı</Th>
                <Th>IBAN</Th>
                <Th>Hesap sahibi</Th>
                <Th>Durum</Th>
                <Th />
              </tr>
            </thead>
            <tbody>
              {hesaplar.length === 0 ? (
                <Bos colSpan={5}>Hesap kaydı bulunamadı.</Bos>
              ) : (
                hesaplar.map((h) => (
                  <Tr key={h.id}>
                    <Td tam className="font-semibold sm:font-normal">{h.ad}</Td>
                    <Td tam etiket="IBAN" className="tabular-nums">{ibanMaske(h.iban)}</Td>
                    <Td etiket="Hesap sahibi">{h.hesapSahibi}</Td>
                    <Td etiket="Durum">
                      <Tag tone={h.durum === 'AKTIF' ? 'accent' : 'neutral'}>
                        {h.durum === 'AKTIF' ? 'Aktif' : 'Pasif'}
                      </Tag>
                    </Td>
                    <Td tam className="text-right">
                      <Button variant="secondary" size="sm" onClick={() => navigate('/hesaplar/' + h.id)}>
                        Güncelle
                      </Button>
                      <Button variant="ghost" size="sm" className="ml-1" onClick={() => setSilinecek(h)}>
                        Sil
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
        open={!!silinecek}
        title="Hesap silinsin mi?"
        description="Bu hesap ödeme ya da öğrenci kayıtlarında kullanılmışsa silinmez, pasife alınır."
        isim={silinecek?.ad}
        confirmLabel="Sil"
        onConfirm={sil}
        onCancel={() => setSilinecek(null)}
      />
    </div>
  );
}
