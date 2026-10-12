import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { ChevronLeft, ChevronRight, Plus, Search } from 'lucide-react';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Seg, SegOpt } from '@/components/ui/seg';
import { Tag } from '@/components/ui/tag';
import { Table, Th, Td, Tr, Bos } from '@/components/ui/table';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import { ogrenciService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata } from '@/context/ToastContext';
import { ODEME_DURUMU_LABEL, tl } from '@/lib/format';

const BOYUT = 10;
const BOS_SAYFA = { icerik: [], sayfa: 0, boyut: BOYUT, toplamKayit: 0, toplamSayfa: 0 };

export default function OgrenciListePage() {
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  // Panelden gelen /ogrenciler?aidat=bekleyen bağlantısı yalnızca aidat bekleyenleri listeler
  const aidatBekleyen = params.get('aidat') === 'bekleyen';
  const [arama, setArama] = useState('');
  const [aramaAktif, setAramaAktif] = useState('');
  const [sayfa, setSayfa] = useState(0);
  const [veri, setVeri] = useState(BOS_SAYFA);
  const [hata, setHata] = useHata();
  const [yukleniyor, setYukleniyor] = useState(true);

  const ara = (e) => {
    e?.preventDefault?.();
    setAramaAktif(arama.trim());
    setSayfa(0);
  };

  const aramaDegisti = (e) => {
    const deger = e.target.value;
    setArama(deger);
    // Input'un X (temizle) işaretine basılınca veya elle tamamen silinince
    // beklemeden arama sıfırlanır, ilk sayfa filtresiz listelenir.
    if (deger === '' && aramaAktif !== '') {
      setAramaAktif('');
      setSayfa(0);
    }
  };

  const yukle = useCallback(() => {
    setYukleniyor(true);
    setHata('');
    ogrenciService
      .list({ q: aramaAktif || undefined, aidatBekleyen: aidatBekleyen || undefined, sayfa, boyut: BOYUT })
      .then(setVeri)
      .catch((e) => setHata(apiHata(e), { sessiz: true }))
      .finally(() => setYukleniyor(false));
  }, [aramaAktif, aidatBekleyen, sayfa]);

  useEffect(() => {
    yukle();
  }, [yukle]);

  const filtreSec = (bekleyen) => {
    setSayfa(0);
    setParams(bekleyen ? { aidat: 'bekleyen' } : {}, { replace: true });
  };

  const toplamSayfa = Math.max(1, veri.toplamSayfa);

  return (
    <div>
      <PageHeader kicker="Okul" baslik="Öğrenciler" />
      <Hata mesaj={hata} onTekrar={yukle} />

      <Card className="gap-3.5">
        <div className="flex flex-wrap items-center gap-2.5">
          <form onSubmit={ara} className="flex w-full gap-2 sm:w-auto">
            <Input
              type="search"
              placeholder="Öğrenci, veli veya grup adıyla ara"
              value={arama}
              onChange={aramaDegisti}
              className="min-w-0 flex-1 sm:w-[340px] sm:flex-none"
            />
            <Button type="submit" variant="secondary">
              <Search size={16} strokeWidth={1.5} />
              <span>Ara</span>
            </Button>
          </form>
          <Seg>
            <SegOpt active={!aidatBekleyen} onClick={() => filtreSec(false)}>
              Tümü
            </SegOpt>
            <SegOpt active={aidatBekleyen} onClick={() => filtreSec(true)}>
              Aidat bekleyen
            </SegOpt>
          </Seg>
          <span className="text-[13.5px] text-ink/55">{veri.toplamKayit} öğrenci</span>
          <Button variant="primary" className="ml-auto" onClick={() => navigate('/ogrenciler/yeni')}>
            <Plus size={16} strokeWidth={1.5} />
            <span>Yeni öğrenci</span>
          </Button>
        </div>

        {yukleniyor ? (
          <Yukleniyor />
        ) : (
          <>
            <Table>
              <thead>
                <tr>
                  <Th>Ad soyad</Th>
                  <Th>Grup</Th>
                  <Th>Veli</Th>
                  <Th>Telefon</Th>
                  <Th className="text-right">Aidat</Th>
                  <Th>Ödeme</Th>
                  <Th>Durum</Th>
                  <Th>Not</Th>
                  <Th />
                </tr>
              </thead>
              <tbody>
                {veri.icerik.length === 0 ? (
                  <Bos colSpan={9}>{aidatBekleyen ? 'Aidat bekleyen öğrenci yok.' : 'Öğrenci kaydı bulunamadı.'}</Bos>
                ) : (
                  veri.icerik.map((o) => (
                    <Tr key={o.id}>
                      <Td>{o.adSoyad}</Td>
                      <Td className="text-ink/55">{o.grupAd || '—'}</Td>
                      <Td>{o.veliAdSoyad || '—'}</Td>
                      <Td className="tabular-nums">{o.veliTelNo || '—'}</Td>
                      <Td className="text-right tabular-nums">{tl(o.aylikAidat)}</Td>
                      <Td>
                        <Tag
                          tone={
                            o.odemeDurumu === 'ODENDI'
                              ? 'accent'
                              : o.odemeDurumu === 'ODENMEDI'
                                ? 'danger'
                                : o.odemeDurumu === 'KISMI_ODENDI'
                                  ? 'warning'
                                  : 'neutral'
                          }
                        >
                          {ODEME_DURUMU_LABEL[o.odemeDurumu] || '—'}
                        </Tag>
                      </Td>
                      <Td>
                        <Tag tone={o.durum === 'AKTIF' ? 'accent' : 'outline'}>
                          {o.durum === 'AKTIF' ? 'Aktif' : 'Pasif'}
                        </Tag>
                      </Td>
                      <Td className="max-w-[220px] truncate text-[13.5px] text-ink/70" title={o.not || ''}>
                        {o.not || '—'}
                      </Td>
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

            <div className="relative flex items-center justify-center gap-3 pt-1">
              <span className="absolute left-0 text-[13.5px] text-ink/55">
                Sayfa {veri.sayfa + 1} / {toplamSayfa}
              </span>
              <div className="flex gap-1.5">
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={sayfa <= 0}
                  onClick={() => setSayfa((p) => Math.max(0, p - 1))}
                >
                  <ChevronLeft size={15} strokeWidth={1.5} />
                  <span>Önceki</span>
                </Button>
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={sayfa + 1 >= toplamSayfa}
                  onClick={() => setSayfa((p) => p + 1)}
                >
                  <span>Sonraki</span>
                  <ChevronRight size={15} strokeWidth={1.5} />
                </Button>
              </div>
            </div>
          </>
        )}
      </Card>
    </div>
  );
}
