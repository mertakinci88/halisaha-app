import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '@/components/AppLayout';
import { Card, CardKicker } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Table, Th, Td, Tr, Bos } from '@/components/ui/table';
import { Tag } from '@/components/ui/tag';
import { Yukleniyor, Hata } from '@/components/ui/durum';
import { envanterService, ogrenciService, rezervasyonService, sahaService, urunService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata } from '@/context/ToastContext';
import { useAuth } from '@/context/AuthContext';
import { hh, saatOf, stokDurumu, tl, toIsoDate } from '@/lib/format';

const ACILIS = 12;
const KAPANIS = 26; // 26 = ertesi gün 02:00 (Takvim ile aynı çalışma saatleri)

export default function DashboardPage() {
  const navigate = useNavigate();
  const { yetkiVar } = useAuth();
  const ogrenciYetkisi = yetkiVar('OGRENCI');
  const urunYetkisi = yetkiVar('URUN');
  const envanterYetkisi = yetkiVar('ENVANTER');
  const [veri, setVeri] = useState(null);
  const [hata, setHata] = useHata();
  const [yukleniyor, setYukleniyor] = useState(true);

  const yukle = () => {
    setYukleniyor(true);
    setHata('');
    Promise.all([
      rezervasyonService.list({ tarih: toIsoDate(new Date()) }),
      sahaService.list(),
      // Yetkisi olmayan menünün verisi çekilmez; kartı da gösterilmez
      ogrenciYetkisi ? ogrenciService.list({ boyut: 1000 }) : Promise.resolve({ icerik: [] }),
      urunYetkisi ? urunService.list() : Promise.resolve([]),
      envanterYetkisi ? envanterService.list() : Promise.resolve([]),
      envanterYetkisi ? envanterService.teslimBekleyenler() : Promise.resolve({ kalemSayisi: 0, ogrenciler: [] }),
    ])
      .then(([rezervasyonlar, sahalar, ogrenciSayfasi, urunler, envanterler, teslimBekleyen]) =>
        setVeri({ rezervasyonlar, sahalar, ogrenciler: ogrenciSayfasi.icerik, urunler, envanterler, teslimBekleyen }),
      )
      .catch((e) => setHata(apiHata(e), { sessiz: true }))
      .finally(() => setYukleniyor(false));
  };

  useEffect(yukle, []);

  const envanterUyarilari = (veri?.envanterler || []).filter((e) => stokDurumu(e));
  const envanterAd = (e) => (e.bedenli && e.beden ? e.turAd + ' (' + e.beden + ')' : e.turAd);

  const kpiler = useMemo(() => {
    if (!veri) return [];
    const aktifRez = veri.rezervasyonlar.filter((r) => r.durum === 'AKTIF');
    const aktifSaha = veri.sahalar.filter((s) => s.durum === 'AKTIF');
    const kapasite = Math.max(1, aktifSaha.length * (KAPANIS - ACILIS + 1));
    const ciro = aktifRez.reduce((t, r) => t + Number(r.odenecekTutar || 0), 0);
    const bekleyen = veri.ogrenciler.filter((o) => o.odemeDurumu !== 'ODENDI');
    const liste = [
      { etiket: 'Bugünkü rezervasyon', deger: String(aktifRez.length), alt: aktifSaha.length + ' aktif saha' },
      {
        etiket: 'Doluluk',
        deger: Math.round((aktifRez.length / kapasite) * 100) + '%',
        alt: hh(ACILIS) + ' – ' + hh(KAPANIS) + ' arası',
      },
      { etiket: 'Bugünkü ciro', deger: tl(ciro), alt: 'Saha + kafeterya' },
    ];
    if (ogrenciYetkisi) {
      liste.push({
        etiket: 'Aidat bekleyen',
        deger: String(bekleyen.length),
        alt: veri.ogrenciler.length + ' kayıtlı öğrenci',
      });
    }
    return liste;
  }, [veri, ogrenciYetkisi]);

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
              {ogrenciYetkisi ? (
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
              ) : null}

              {envanterYetkisi ? (
                <Card>
                  <h4 className="text-[22px]">Envanter</h4>

                  <div className="flex flex-col gap-2 border border-divider bg-surface/40 p-3.5">
                    <h5 className="font-heading text-[18px] font-semibold">Stoğu azalan envanterler</h5>
                    <Table>
                      <tbody>
                        {envanterUyarilari.length === 0 ? (
                          <Bos colSpan={2}>Envanter stokları yeterli.</Bos>
                        ) : (
                          envanterUyarilari.slice(0, 5).map((e) => (
                            <Tr key={e.id}>
                              <Td>{envanterAd(e)}</Td>
                              <Td className="text-right">
                                {stokDurumu(e) === 'TUKENDI' ? (
                                  <Tag tone="danger">Tükendi</Tag>
                                ) : (
                                  <Tag tone="neutral">{e.adet} adet kaldı</Tag>
                                )}
                              </Td>
                            </Tr>
                          ))
                        )}
                      </tbody>
                    </Table>
                    {envanterUyarilari.length > 5 ? (
                      <div className="text-[13.5px] text-ink/55">+{envanterUyarilari.length - 5} envanter daha</div>
                    ) : null}
                  </div>

                  <div className="flex flex-col gap-2 border border-divider bg-surface/40 p-3.5">
                    <div className="flex items-baseline gap-2.5">
                      <h5 className="font-heading text-[18px] font-semibold">Teslim bekleyen öğrenciler</h5>
                      {veri.teslimBekleyen.kalemSayisi > 0 ? (
                        <span className="ml-auto text-[13.5px] tabular-nums text-ink/60">{veri.teslimBekleyen.kalemSayisi} envanter</span>
                      ) : null}
                    </div>
                    <Table>
                      <tbody>
                        {veri.teslimBekleyen.ogrenciler.length === 0 ? (
                          <Bos colSpan={2}>Teslim bekleyen öğrenci yok.</Bos>
                        ) : (
                          veri.teslimBekleyen.ogrenciler.slice(0, 5).map((o) => (
                            <Tr
                              key={o.ogrenciId}
                              className={ogrenciYetkisi ? 'cursor-pointer' : undefined}
                              onClick={ogrenciYetkisi ? () => navigate('/ogrenciler/' + o.ogrenciId) : undefined}
                            >
                              <Td>{o.adSoyad}</Td>
                              <Td className="text-right text-[13.5px] text-ink/70">
                                {o.kalemler.map((k) => envanterAd(k)).join(', ')}
                              </Td>
                            </Tr>
                          ))
                        )}
                      </tbody>
                    </Table>
                    {veri.teslimBekleyen.ogrenciler.length > 5 ? (
                      <div className="text-[13.5px] text-ink/55">+{veri.teslimBekleyen.ogrenciler.length - 5} öğrenci daha</div>
                    ) : null}
                  </div>

                  <Button variant="secondary" className="self-start" onClick={() => navigate('/envanter')}>
                    Envantere git
                  </Button>
                </Card>
              ) : null}

              {urunYetkisi ? (
              <Card>
                <h4 className="text-[22px]">Stoğu azalan ürünler</h4>
                <Table>
                  <tbody>
                    {veri.urunler.filter((u) => stokDurumu(u)).length === 0 ? (
                      <Bos colSpan={2}>Stok seviyeleri yeterli.</Bos>
                    ) : (
                      veri.urunler
                        .filter((u) => stokDurumu(u))
                        .map((u) => (
                          <Tr key={u.id}>
                            <Td>{u.ad}</Td>
                            <Td className="text-right">
                              {stokDurumu(u) === 'TUKENDI' ? (
                                <Tag tone="danger">Tükendi</Tag>
                              ) : (
                                <Tag tone="neutral">{u.adet} adet kaldı</Tag>
                              )}
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
              ) : null}
            </div>
          </div>
        </>
      ) : null}
    </div>
  );
}
