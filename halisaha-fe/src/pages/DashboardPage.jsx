import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '@/components/AppLayout';
import { Card, CardKicker } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Table, Th, Td, Tr, Bos } from '@/components/ui/table';
import { Tag } from '@/components/ui/tag';
import { Yukleniyor, Hata } from '@/components/ui/durum';
import { ogrenciService, rezervasyonService, sahaService, urunService } from '@/services';
import { apiHata } from '@/lib/api';
import { hh, saatOf, tl, toIsoDate } from '@/lib/format';

const ACILIS = 9;
const KAPANIS = 23;

export default function DashboardPage() {
  const navigate = useNavigate();
  const [veri, setVeri] = useState(null);
  const [hata, setHata] = useState('');
  const [yukleniyor, setYukleniyor] = useState(true);

  const yukle = () => {
    setYukleniyor(true);
    setHata('');
    Promise.all([
      rezervasyonService.list({ tarih: toIsoDate(new Date()) }),
      sahaService.list(),
      ogrenciService.list(),
      urunService.list(),
    ])
      .then(([rezervasyonlar, sahalar, ogrenciler, urunler]) =>
        setVeri({ rezervasyonlar, sahalar, ogrenciler, urunler }),
      )
      .catch((e) => setHata(apiHata(e)))
      .finally(() => setYukleniyor(false));
  };

  useEffect(yukle, []);

  const kpiler = useMemo(() => {
    if (!veri) return [];
    const aktifRez = veri.rezervasyonlar.filter((r) => r.durum === 'AKTIF');
    const aktifSaha = veri.sahalar.filter((s) => s.durum === 'AKTIF');
    const kapasite = Math.max(1, aktifSaha.length * (KAPANIS - ACILIS + 1));
    const ciro = aktifRez.reduce((t, r) => t + Number(r.odenecekTutar || 0), 0);
    const bekleyen = veri.ogrenciler.filter((o) => o.odemeDurumu !== 'ODENDI');
    return [
      { etiket: 'Bugünkü rezervasyon', deger: String(aktifRez.length), alt: aktifSaha.length + ' aktif saha' },
      {
        etiket: 'Doluluk',
        deger: Math.round((aktifRez.length / kapasite) * 100) + '%',
        alt: hh(ACILIS) + ' – ' + hh(KAPANIS) + ' arası',
      },
      { etiket: 'Bugünkü ciro', deger: tl(ciro), alt: 'Saha + kafeterya' },
      { etiket: 'Aidat bekleyen', deger: String(bekleyen.length), alt: veri.ogrenciler.length + ' kayıtlı öğrenci' },
    ];
  }, [veri]);

  return (
    <div>
      <PageHeader kicker="Genel görünüm" baslik="Panel" />
      <Hata mesaj={hata} onTekrar={yukle} />
      {yukleniyor ? <Yukleniyor /> : null}

      {veri ? (
        <>
          <div className="mb-7 grid gap-4 [grid-template-columns:repeat(auto-fit,minmax(190px,1fr))]">
            {kpiler.map((k) => (
              <Card key={k.etiket} className="gap-1.5 px-[18px] py-4">
                <CardKicker>{k.etiket}</CardKicker>
                <div className="font-heading text-[38px] font-semibold leading-none">{k.deger}</div>
                <div className="text-[13.5px] text-ink/55">{k.alt}</div>
              </Card>
            ))}
          </div>

          <div className="grid gap-5 [grid-template-columns:repeat(auto-fit,minmax(320px,1fr))]">
            <Card>
              <div className="flex items-baseline gap-2.5">
                <h4 className="text-[22px]">Bugünün programı</h4>
                <Button variant="ghost" className="ml-auto" onClick={() => navigate('/takvim')}>
                  Tümü
                </Button>
              </div>
              <Table>
                <thead>
                  <tr>
                    <Th>Saat</Th>
                    <Th>Saha</Th>
                    <Th>Ad soyad</Th>
                    <Th className="text-right">Tutar</Th>
                  </tr>
                </thead>
                <tbody>
                  {veri.rezervasyonlar.filter((r) => r.durum === 'AKTIF').length === 0 ? (
                    <Bos colSpan={4}>Bugün için rezervasyon yok.</Bos>
                  ) : (
                    veri.rezervasyonlar
                      .filter((r) => r.durum === 'AKTIF')
                      .sort((a, b) => String(a.baslangicTarih).localeCompare(String(b.baslangicTarih)))
                      .map((r) => (
                        <Tr
                          key={r.id}
                          className="cursor-pointer"
                          onClick={() => navigate('/rezervasyon/' + r.id)}
                        >
                          <Td className="whitespace-nowrap tabular-nums">
                            {saatOf(r.baslangicTarih)} – {saatOf(r.bitisTarih)}
                          </Td>
                          <Td className="text-ink/55">{r.sahaAd}</Td>
                          <Td>{r.adSoyad}</Td>
                          <Td className="text-right tabular-nums">{tl(r.odenecekTutar)}</Td>
                        </Tr>
                      ))
                  )}
                </tbody>
              </Table>
            </Card>

            <div className="grid content-start gap-5">
              <Card>
                <h4 className="text-[22px]">Aidat bekleyen öğrenciler</h4>
                <Table>
                  <tbody>
                    {veri.ogrenciler.filter((o) => o.odemeDurumu !== 'ODENDI').length === 0 ? (
                      <Bos colSpan={3}>Bekleyen aidat yok.</Bos>
                    ) : (
                      veri.ogrenciler
                        .filter((o) => o.odemeDurumu !== 'ODENDI')
                        .slice(0, 5)
                        .map((o) => (
                          <Tr key={o.id}>
                            <Td>{o.adSoyad}</Td>
                            <Td className="text-[13.5px] text-ink/55">{o.grupAd || '—'}</Td>
                            <Td className="text-right tabular-nums">{tl(o.aylikAidat)}</Td>
                          </Tr>
                        ))
                    )}
                  </tbody>
                </Table>
                <Button variant="secondary" className="self-start" onClick={() => navigate('/ogrenciler')}>
                  Öğrenci listesi
                </Button>
              </Card>

              <Card>
                <h4 className="text-[22px]">Stoğu azalan ürünler</h4>
                <Table>
                  <tbody>
                    {veri.urunler.filter((u) => u.adet <= 10).length === 0 ? (
                      <Bos colSpan={2}>Stok seviyeleri yeterli.</Bos>
                    ) : (
                      veri.urunler
                        .filter((u) => u.adet <= 10)
                        .map((u) => (
                          <Tr key={u.id}>
                            <Td>{u.ad}</Td>
                            <Td className="text-right">
                              <Tag tone="neutral">{u.adet} adet kaldı</Tag>
                            </Td>
                          </Tr>
                        ))
                    )}
                  </tbody>
                </Table>
                <Button variant="secondary" className="self-start" onClick={() => navigate('/urunler')}>
                  Kafeteryaya git
                </Button>
              </Card>
            </div>
          </div>
        </>
      ) : null}
    </div>
  );
}
