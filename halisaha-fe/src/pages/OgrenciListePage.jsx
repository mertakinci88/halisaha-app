import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus } from 'lucide-react';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Tag } from '@/components/ui/tag';
import { Table, Th, Td, Tr, Bos } from '@/components/ui/table';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import { ogrenciService } from '@/services';
import { apiHata } from '@/lib/api';
import { ODEME_DURUMU_LABEL, kisaTarih, tl } from '@/lib/format';

export default function OgrenciListePage() {
  const navigate = useNavigate();
  const [ogrenciler, setOgrenciler] = useState([]);
  const [arama, setArama] = useState('');
  const [hata, setHata] = useState('');
  const [yukleniyor, setYukleniyor] = useState(true);

  const yukle = () => {
    setYukleniyor(true);
    setHata('');
    ogrenciService
      .list()
      .then(setOgrenciler)
      .catch((e) => setHata(apiHata(e)))
      .finally(() => setYukleniyor(false));
  };

  useEffect(yukle, []);

  const filtreli = useMemo(() => {
    const q = arama.trim().toLocaleLowerCase('tr');
    if (!q) return ogrenciler;
    return ogrenciler.filter(
      (o) =>
        (o.adSoyad || '').toLocaleLowerCase('tr').includes(q) ||
        (o.veliAdSoyad || '').toLocaleLowerCase('tr').includes(q) ||
        (o.grupAd || '').toLocaleLowerCase('tr').includes(q),
    );
  }, [ogrenciler, arama]);

  return (
    <div>
      <PageHeader kicker="Okul" baslik="Öğrenciler" />
      <Hata mesaj={hata} onTekrar={yukle} />

      <Card className="gap-3.5">
        <div className="flex flex-wrap items-center gap-2.5">
          <Input
            type="search"
            placeholder="Öğrenci, veli veya grup adıyla ara"
            value={arama}
            onChange={(e) => setArama(e.target.value)}
            className="max-w-[300px]"
          />
          <span className="text-[13.5px] text-ink/55">
            {filtreli.length} / {ogrenciler.length} öğrenci
          </span>
          <Button variant="primary" className="ml-auto" onClick={() => navigate('/ogrenciler/yeni')}>
            <Plus size={16} strokeWidth={1.5} />
            <span>Yeni öğrenci</span>
          </Button>
        </div>

        {yukleniyor ? (
          <Yukleniyor />
        ) : (
          <Table>
            <thead>
              <tr>
                <Th>Ad soyad</Th>
                <Th>Grup</Th>
                <Th>Veli</Th>
                <Th>Telefon</Th>
                <Th className="text-right">Aidat</Th>
                <Th>Ödeme</Th>
                <Th>Sonraki ödeme</Th>
                <Th />
              </tr>
            </thead>
            <tbody>
              {filtreli.length === 0 ? (
                <Bos colSpan={8}>Öğrenci kaydı bulunamadı.</Bos>
              ) : (
                filtreli.map((o) => (
                  <Tr key={o.id}>
                    <Td>{o.adSoyad}</Td>
                    <Td className="text-ink/55">{o.grupAd || '—'}</Td>
                    <Td>{o.veliAdSoyad || '—'}</Td>
                    <Td className="tabular-nums">{o.veliTelNo || '—'}</Td>
                    <Td className="text-right tabular-nums">{tl(o.aylikAidat)}</Td>
                    <Td>
                      <Tag tone={o.odemeDurumu === 'ODENDI' ? 'accent' : 'neutral'}>
                        {ODEME_DURUMU_LABEL[o.odemeDurumu] || '—'}
                      </Tag>
                    </Td>
                    <Td className="tabular-nums text-ink/55">{kisaTarih(o.sonrakiOdemeTarih)}</Td>
                    <Td className="text-right">
                      <Button variant="secondary" size="sm" onClick={() => navigate('/ogrenciler/' + o.id)}>
                        Güncelle
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
