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
import { rezervasyonService, sahaService } from '@/services';
import { apiHata } from '@/lib/api';
import { tl } from '@/lib/format';

export default function SahaListePage() {
  const navigate = useNavigate();
  const [sahalar, setSahalar] = useState([]);
  const [hata, setHata] = useState('');
  const [yukleniyor, setYukleniyor] = useState(true);
  const [silinecek, setSilinecek] = useState(null);
  const [pasifUyarisi, setPasifUyarisi] = useState(null);
  const [gecmisUyarisi, setGecmisUyarisi] = useState(null);
  const [kontrolEdilenId, setKontrolEdilenId] = useState(null);

  const yukle = () => {
    setYukleniyor(true);
    setHata('');
    sahaService
      .list()
      .then(setSahalar)
      .catch((e) => setHata(apiHata(e)))
      .finally(() => setYukleniyor(false));
  };

  useEffect(yukle, []);

  const silBaslat = async (s) => {
    setKontrolEdilenId(s.id);
    try {
      const rezervasyonlar = await rezervasyonService.list({ sahaId: s.id });
      if (rezervasyonlar.some((r) => r.durum === 'AKTIF')) {
        setPasifUyarisi(s);
      } else if (rezervasyonlar.length > 0) {
        setGecmisUyarisi(s);
      } else {
        setSilinecek(s);
      }
    } catch (e) {
      setHata(apiHata(e));
    } finally {
      setKontrolEdilenId(null);
    }
  };

  const sil = async () => {
    const s = silinecek || pasifUyarisi || gecmisUyarisi;
    setSilinecek(null);
    setPasifUyarisi(null);
    setGecmisUyarisi(null);
    try {
      await sahaService.remove(s.id);
      yukle();
    } catch (e) {
      setHata(apiHata(e));
    }
  };

  return (
    <div>
      <PageHeader kicker="Tanımlar" baslik="Sahalar" />
      <Hata mesaj={hata} onTekrar={yukle} />

      <Card className="gap-3.5">
        <div className="flex flex-wrap items-center gap-2.5">
          <h4 className="text-[22px]">Saha listesi</h4>
          <Button variant="primary" className="ml-auto" onClick={() => navigate('/sahalar/yeni')}>
            <Plus size={16} strokeWidth={1.5} />
            <span>Yeni saha</span>
          </Button>
        </div>

        {yukleniyor ? (
          <Yukleniyor />
        ) : (
          <Table>
            <thead>
              <tr>
                <Th>Saha adı</Th>
                <Th>Durum</Th>
                <Th className="text-right">Saatlik ücret</Th>
                <Th />
              </tr>
            </thead>
            <tbody>
              {sahalar.length === 0 ? (
                <Bos colSpan={4}>Saha kaydı bulunamadı.</Bos>
              ) : (
                sahalar.map((s) => (
                  <Tr key={s.id}>
                    <Td>{s.ad}</Td>
                    <Td>
                      <Tag tone={s.durum === 'AKTIF' ? 'accent' : 'neutral'}>
                        {s.durum === 'AKTIF' ? 'Aktif' : 'Pasif'}
                      </Tag>
                    </Td>
                    <Td className="text-right tabular-nums">{tl(s.saatlikUcret)}</Td>
                    <Td className="text-right">
                      <Button variant="secondary" size="sm" onClick={() => navigate('/sahalar/' + s.id)}>
                        Güncelle
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="ml-1"
                        disabled={kontrolEdilenId === s.id}
                        onClick={() => silBaslat(s)}
                      >
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
        title="Saha silinsin mi?"
        description="Bu işlem geri alınamaz."
        isim={silinecek?.ad}
        confirmLabel="Sil"
        onConfirm={sil}
        onCancel={() => setSilinecek(null)}
      />

      <ConfirmDialog
        open={!!pasifUyarisi}
        title="Bu sahaya ait rezervasyonlar bulunuyor"
        description="Silmeniz durumunda saha pasif duruma geçecektir. Pasif duruma çekilen sahalar rezervasyon ve takvim ekranlarında gösterilmez. Emin misiniz?"
        isim={pasifUyarisi?.ad}
        confirmLabel="Pasife al"
        onConfirm={sil}
        onCancel={() => setPasifUyarisi(null)}
      />

      <ConfirmDialog
        open={!!gecmisUyarisi}
        title="Bu sahaya ait geçmiş rezervasyon kayıtları bulunuyor"
        description="Silmeniz durumunda saha pasif duruma alınacaktır. Emin misiniz?"
        isim={gecmisUyarisi?.ad}
        confirmLabel="Pasife al"
        onConfirm={sil}
        onCancel={() => setGecmisUyarisi(null)}
      />
    </div>
  );
}
