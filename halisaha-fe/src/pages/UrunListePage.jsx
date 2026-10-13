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
import { urunService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata, useToast } from '@/context/ToastContext';
import { stokDurumu, tl } from '@/lib/format';

export default function UrunListePage() {
  const navigate = useNavigate();
  const [urunler, setUrunler] = useState([]);
  const [hata, setHata] = useHata();
  const toast = useToast();
  const [yukleniyor, setYukleniyor] = useState(true);
  const [silinecek, setSilinecek] = useState(null);

  const yukle = () => {
    setYukleniyor(true);
    setHata('');
    urunService
      .list()
      .then(setUrunler)
      .catch((e) => setHata(apiHata(e), { sessiz: true }))
      .finally(() => setYukleniyor(false));
  };

  useEffect(yukle, []);

  const sil = async () => {
    const u = silinecek;
    setSilinecek(null);
    try {
      await urunService.remove(u.id);
      toast.basari('Ürün silindi.');
      yukle();
    } catch (e) {
      setHata(apiHata(e));
    }
  };

  return (
    <div>
      <PageHeader kicker="Tanımlar" baslik="Kafeterya ürünleri" />
      <Hata mesaj={hata} onTekrar={yukle} />

      <Card className="gap-3.5">
        <div className="flex flex-wrap items-center gap-2.5">
          <h4 className="text-[22px]">Ürün listesi</h4>
          <Button variant="primary" className="ml-auto" onClick={() => navigate('/urunler/yeni')}>
            <Plus size={16} strokeWidth={1.5} />
            <span>Yeni ürün</span>
          </Button>
        </div>

        {yukleniyor ? (
          <Yukleniyor />
        ) : (
          <Table kart>
            <thead>
              <tr>
                <Th>Ürün</Th>
                <Th className="text-right">Fiyat</Th>
                <Th className="text-right">Stok</Th>
                <Th>Uyarı</Th>
                <Th />
              </tr>
            </thead>
            <tbody>
              {urunler.length === 0 ? (
                <Bos colSpan={5}>Ürün kaydı bulunamadı.</Bos>
              ) : (
                urunler.map((u) => (
                  <Tr key={u.id}>
                    <Td tam className="font-semibold sm:font-normal">{u.ad}</Td>
                    <Td etiket="Fiyat" className="text-right tabular-nums">{tl(u.fiyat)}</Td>
                    <Td etiket="Stok" className="text-right tabular-nums">{u.adet}</Td>
                    <Td>
                      {stokDurumu(u) === 'TUKENDI' ? (
                        <Tag tone="danger">Tükendi</Tag>
                      ) : stokDurumu(u) === 'AZ' ? (
                        <Tag tone="outline">Stok az</Tag>
                      ) : null}
                    </Td>
                    <Td tam className="whitespace-nowrap text-right">
                      <Button variant="guncelle" size="sm" onClick={() => navigate('/urunler/' + u.id)}>
                        Güncelle
                      </Button>
                      <Button variant="danger" size="sm" className="ml-1" onClick={() => setSilinecek(u)}>
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
        title="Ürün silinsin mi?"
        description="Bu işlem geri alınamaz."
        isim={silinecek?.ad}
        confirmLabel="Sil"
        tehlikeli
        onConfirm={sil}
        onCancel={() => setSilinecek(null)}
      />
    </div>
  );
}
