import { useEffect, useMemo, useState } from 'react';
import { ArrowRight } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '@/components/AppLayout';
import { Card, CardKicker } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Table, Th, Td, Tr, Bos } from '@/components/ui/table';
import { Tag } from '@/components/ui/tag';
import { Yukleniyor, Hata } from '@/components/ui/durum';
import { envanterService, ogrenciService, rezervasyonService, sahaService, urunService } from '@/services';
import { apiHata } from '@/lib/api';
import { cn } from '@/lib/utils';
import { useHata } from '@/context/ToastContext';
import { useAuth } from '@/context/AuthContext';
import { birimAd } from '@/lib/envanter';
import { hh, saatOf, saatYazi, stokDurumu, sureSaat, tl, toIsoDate } from '@/lib/format';

const ACILIS = 12;
const KAPANIS = 26; // 26 = ertesi gün 02:00 (Takvim ile aynı çalışma saatleri)

/** Doluluk oranını (0–100) gösteren ilerleme çubuğu. */
function DolulukCubugu({ oran, etiket, ince = false }) {
  return (
    <div
      role="progressbar"
      aria-label={etiket}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={oran}
      className={cn('w-full overflow-hidden bg-ink/10', ince ? 'h-1.5' : 'h-2')}
    >
      <div className="h-full bg-accent transition-[width] duration-500" style={{ width: oran + '%' }} />
    </div>
  );
}

// Ödemesiz (ödeme alınmayan) öğrenciler bekleyen sayılmaz
const odemeBekliyor = (o) => o.odemeAlinir && o.odemeDurumu !== 'ODENDI';

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
      ogrenciYetkisi ? ogrenciService.list({ durum: 'AKTIF', boyut: 1000 }) : Promise.resolve({ icerik: [] }),
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
    // Doluluk rezervasyon sayısına değil dolu saat sayısına göre hesaplanır (3 saatlik kayıt 3 saat sayılır)
    const aktifSahaIdleri = new Set(aktifSaha.map((s) => s.id));
    const doluSaat = aktifRez
      .filter((r) => aktifSahaIdleri.has(r.sahaId))
      .reduce((t, r) => t + sureSaat(r.baslangicTarih, r.bitisTarih), 0);
    const dolulukOrani = Math.min(100, Math.round((doluSaat / kapasite) * 100));
    const sahaKapasitesi = KAPANIS - ACILIS + 1;
    const sahaDoluluklari = aktifSaha.map((sh) => {
      const saat = aktifRez
        .filter((r) => r.sahaId === sh.id)
        .reduce((t, r) => t + sureSaat(r.baslangicTarih, r.bitisTarih), 0);
      return { id: sh.id, ad: sh.ad, saat, kapasite: sahaKapasitesi, oran: Math.min(100, Math.round((saat / sahaKapasitesi) * 100)) };
    });
    const ciro = aktifRez.reduce((t, r) => t + Number(r.odenecekTutar || 0), 0);
    const bekleyen = veri.ogrenciler.filter(odemeBekliyor);
    const liste = [
      { etiket: 'Bugünkü rezervasyon', deger: String(aktifRez.length), alt: aktifSaha.length + ' aktif saha' },
      {
        etiket: 'Bugünkü doluluk',
        deger: dolulukOrani + '%',
        oran: dolulukOrani,
        sahalar: sahaDoluluklari,
        alt: saatYazi(doluSaat) + ' / ' + kapasite + ' saat · ' + hh(ACILIS) + ' – ' + hh(KAPANIS),
      },
      { etiket: 'Bugünkü ciro', deger: tl(ciro), alt: 'Saha + kafeterya' },
    ];
    if (ogrenciYetkisi) {
      liste.push({
        etiket: 'Ödeme bekleyen',
        deger: String(bekleyen.length),
        alt: veri.ogrenciler.filter((o) => o.odemeAlinir).length + ' ödeme alınan aktif öğrenci',
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
              <Card
                key={k.etiket}
                className={cn('gap-1.5 px-[18px] py-4', k.sahalar?.length > 0 && 'sm:col-span-2 sm:flex-row sm:gap-6')}
              >
                <div className="grid content-start gap-1.5 sm:w-[190px] sm:shrink-0">
                  <CardKicker>{k.etiket}</CardKicker>
                  <div className="font-heading text-[38px] font-semibold leading-none">{k.deger}</div>
                  {k.oran != null ? <DolulukCubugu oran={k.oran} etiket={k.etiket} /> : null}
                  <div className="text-[13.5px] text-ink/55">{k.alt}</div>
                </div>
                {k.sahalar?.length > 0 ? (
                  <div className="mt-2 grid min-w-0 flex-1 content-center gap-2 border-t border-hairline pt-3 sm:mt-0 sm:border-l sm:border-t-0 sm:pl-6 sm:pt-0">
                    {k.sahalar.map((sh) => (
                      <div key={sh.id} className="grid items-center gap-x-3 text-[14px] [grid-template-columns:minmax(0,7rem)_1fr_auto]">
                        <span className="truncate">{sh.ad}</span>
                        <DolulukCubugu oran={sh.oran} etiket={sh.ad + ' doluluk'} ince />
                        <span className="tabular-nums text-ink/55">
                          {sh.saat}/{sh.kapasite} · %{sh.oran}
                        </span>
                      </div>
                    ))}
                  </div>
                ) : null}
              </Card>
            ))}
          </div>

          <div className="grid gap-5 [grid-template-columns:repeat(auto-fit,minmax(min(320px,100%),1fr))]">
            <Card>
              <div className="flex items-baseline gap-2.5">
                <h4 className="text-[22px]">Bugünün programı</h4>
                <Button variant="gri" className="ml-auto" onClick={() => navigate('/takvim')}>
                  <span>Tümü</span>
                  <ArrowRight size={16} strokeWidth={1.5} />
                </Button>
              </div>
              <Table kart>
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
                          <Td etiket="Saat" className="whitespace-nowrap tabular-nums">
                            {saatOf(r.baslangicTarih)} – {saatOf(r.bitisTarih)}
                          </Td>
                          <Td etiket="Saha" className="text-ink/55">{r.sahaAd}</Td>
                          <Td etiket="Ad soyad">{r.adSoyad}</Td>
                          <Td etiket="Tutar" className="text-right tabular-nums">{tl(r.odenecekTutar)}</Td>
                        </Tr>
                      ))
                  )}
                </tbody>
              </Table>
            </Card>

            <div className="grid content-start gap-5">
              {ogrenciYetkisi ? (
              <Card>
                <h4 className="text-[22px]">Ödeme bekleyen öğrenciler</h4>
                <Table>
                  <tbody>
                    {veri.ogrenciler.filter(odemeBekliyor).length === 0 ? (
                      <Bos colSpan={2}>Bekleyen ödeme yok.</Bos>
                    ) : (
                      veri.ogrenciler
                        .filter((o) => o.odemeDurumu !== 'ODENDI')
                        .slice(0, 5)
                        .map((o) => (
                          <Tr key={o.id} className="cursor-pointer" onClick={() => navigate('/ogrenciler/' + o.id)}>
                            <Td>{o.adSoyad}</Td>
                            <Td className="text-[13.5px] text-ink/55">{o.grupAd || '—'}</Td>
                          </Tr>
                        ))
                    )}
                  </tbody>
                </Table>
                <Button variant="gri" className="self-start" onClick={() => navigate('/ogrenciler?odeme=bekleyen')}>
                  <span>Ödeme bekleyen tüm öğrenciler</span>
                  <ArrowRight size={16} strokeWidth={1.5} />
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
                            <Tr key={e.id} className="cursor-pointer" onClick={() => navigate('/envanter/' + e.id)}>
                              <Td>{envanterAd(e)}</Td>
                              <Td className="text-right">
                                {stokDurumu(e) === 'TUKENDI' ? (
                                  <Tag tone="danger">Tükendi</Tag>
                                ) : (
                                  <Tag tone="neutral">{e.adet} {birimAd(e.tur)} kaldı</Tag>
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

                  <Button variant="gri" className="self-start" onClick={() => navigate('/envanter')}>
                    <span>Envantere git</span>
                    <ArrowRight size={16} strokeWidth={1.5} />
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
                          <Tr key={u.id} className="cursor-pointer" onClick={() => navigate('/urunler/' + u.id)}>
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
                <Button variant="gri" className="self-start" onClick={() => navigate('/urunler')}>
                  <span>Kafeteryaya git</span>
                  <ArrowRight size={16} strokeWidth={1.5} />
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
