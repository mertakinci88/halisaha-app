import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus } from 'lucide-react';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Table, Th, Td, Tr, Bos } from '@/components/ui/table';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import { ConfirmDialog } from '@/components/ui/dialog';
import { grupService, ogrenciService } from '@/services';
import { apiHata } from '@/lib/api';

export default function GrupListePage() {
  const navigate = useNavigate();
  const [gruplar, setGruplar] = useState([]);
  const [ogrenciler, setOgrenciler] = useState([]);
  const [hata, setHata] = useState('');
  const [yukleniyor, setYukleniyor] = useState(true);
  const [silinecek, setSilinecek] = useState(null);

  const yukle = () => {
    setYukleniyor(true);
    setHata('');
    Promise.all([grupService.list(), ogrenciService.list()])
      .then(([g, o]) => {
        setGruplar(g);
        setOgrenciler(o);
      })
      .catch((e) => setHata(apiHata(e)))
      .finally(() => setYukleniyor(false));
  };

  useEffect(yukle, []);

  const sil = async () => {
    const g = silinecek;
    setSilinecek(null);
    try {
      await grupService.remove(g.id);
      yukle();
    } catch (e) {
      setHata(apiHata(e));
    }
  };

  return (
    <div>
      <PageHeader kicker="Okul" baslik="Gruplar" />
      <Hata mesaj={hata} onTekrar={yukle} />

      <Card className="gap-3.5">
        <div className="flex flex-wrap items-center gap-2.5">
          <h4 className="text-[22px]">Grup listesi</h4>
          <Button variant="primary" className="ml-auto" onClick={() => navigate('/gruplar/yeni')}>
            <Plus size={16} strokeWidth={1.5} />
            <span>Yeni grup</span>
          </Button>
        </div>

        {yukleniyor ? (
          <Yukleniyor />
        ) : (
          <Table>
            <thead>
              <tr>
                <Th>Grup adı</Th>
                <Th className="text-right">Öğrenci sayısı</Th>
                <Th />
              </tr>
            </thead>
            <tbody>
              {gruplar.length === 0 ? (
                <Bos colSpan={3}>Grup kaydı bulunamadı.</Bos>
              ) : (
                gruplar.map((g) => (
                  <Tr key={g.id}>
                    <Td>{g.ad}</Td>
                    <Td className="text-right tabular-nums">
                      {ogrenciler.filter((o) => o.grupId === g.id).length}
                    </Td>
                    <Td className="text-right">
                      <Button variant="secondary" size="sm" onClick={() => navigate('/gruplar/' + g.id)}>
                        Güncelle
                      </Button>
                      <Button variant="ghost" size="sm" className="ml-1" onClick={() => setSilinecek(g)}>
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
        title="Grup silinsin mi?"
        description="Bu işlem geri alınamaz."
        isim={silinecek?.ad}
        confirmLabel="Sil"
        onConfirm={sil}
        onCancel={() => setSilinecek(null)}
      />
    </div>
  );
}
