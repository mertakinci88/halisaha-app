import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { ChevronLeft, ChevronRight, Plus, Search } from 'lucide-react';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Seg, SegOpt } from '@/components/ui/seg';
import OdemeDurumuEtiketi from '@/components/OdemeDurumuEtiketi';
import { Tag } from '@/components/ui/tag';
import { Table, Th, Td, Tr, Bos } from '@/components/ui/table';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import { ogrenciService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata } from '@/context/ToastContext';
import { tl } from '@/lib/format';

const BOYUT = 10;
const BOS_SAYFA = { icerik: [], sayfa: 0, boyut: BOYUT, toplamKayit: 0, toplamSayfa: 0 };

export default function OgrenciListePage() {
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  // Panelden gelen /ogrenciler?odeme=bekleyen bağlantısı yalnızca ödeme bekleyenleri listeler
  const odemeBekleyen = params.get('odeme') === 'bekleyen';
  // ?envanter=eksik: ödemesi alınmış ama teslim edilmemiş envanteri olan öğrenciler
  const envanterEksik = params.get('envanter') === 'eksik';
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
      .list({ q: aramaAktif || undefined, odemeBekleyen: odemeBekleyen || undefined, envanterEksik: envanterEksik || undefined, sayfa, boyut: BOYUT })
      .then(setVeri)
      .catch((e) => setHata(apiHata(e), { sessiz: true }))
      .finally(() => setYukleniyor(false));
  }, [aramaAktif, odemeBekleyen, envanterEksik, sayfa]);

  useEffect(() => {
    yukle();
  }, [yukle]);

  const filtreSec = (filtre) => {
    setSayfa(0);
    setParams(
      filtre === 'odeme' ? { odeme: 'bekleyen' } : filtre === 'envanter' ? { envanter: 'eksik' } : {},
      { replace: true },
    );
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
            <SegOpt active={!odemeBekleyen && !envanterEksik} onClick={() => filtreSec(null)}>
              Tümü
            </SegOpt>
            <SegOpt active={odemeBekleyen} onClick={() => filtreSec('odeme')}>
              Ödeme bekleyen
            </SegOpt>
            <SegOpt active={envanterEksik} onClick={() => filtreSec('envanter')}>
              Eksik envanter
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
            <Table kart>
              <thead>
                <tr>
                  <Th>Ad soyad</Th>
                  <Th>Grup</Th>
                  <Th>Veli</Th>
                  <Th>Telefon</Th>
                  <Th>Bu ayki ödeme</Th>
                  <Th>Durum</Th>
                  <Th>Not</Th>
                  <Th />
                </tr>
              </thead>
              <tbody>
                {veri.icerik.length === 0 ? (
                  <Bos colSpan={8}>{odemeBekleyen ? 'Ödeme bekleyen öğrenci yok.' : envanterEksik ? 'Eksik envanteri olan öğrenci yok.' : 'Öğrenci kaydı bulunamadı.'}</Bos>
                ) : (
                  veri.icerik.map((o) => (
                    <Tr key={o.id} className="cursor-pointer" onClick={() => navigate('/ogrenciler/' + o.id)}>
                      <Td tam className="font-semibold sm:font-normal">{o.adSoyad}</Td>
                      <Td etiket="Grup" className="text-ink/55">{o.grupAd || '—'}</Td>
                      <Td etiket="Veli">{o.veliAdSoyad || '—'}</Td>
                      <Td etiket="Telefon" className="tabular-nums">{o.veliTelNo || '—'}</Td>
                      <Td etiket="Bu ayki ödeme">
                        <OdemeDurumuEtiketi durum={o.odemeDurumu} />
                      </Td>
                      <Td etiket="Durum">
                        <Tag tone={o.durum === 'AKTIF' ? 'accent' : 'outline'}>
                          {o.durum === 'AKTIF' ? 'Aktif' : 'Pasif'}
                        </Tag>
                      </Td>
                      <Td tam etiket="Not" className="max-sm:order-1 max-w-[220px] truncate text-[13.5px] text-ink/70" title={o.not || ''}>
                        {o.not || '—'}
                      </Td>
                      <Td className="text-right">
                        <Button
                          variant="primary"
                          size="sm"
                          onClick={(e) => {
                            e.stopPropagation();
                            navigate('/ogrenciler/' + o.id);
                          }}
                        >
                          Detay
                        </Button>
                      </Td>
                    </Tr>
                  ))
                )}
              </tbody>
            </Table>

            <div className="relative flex flex-col items-center gap-2 pt-1 sm:flex-row sm:justify-center sm:gap-3">
              <span className="order-2 text-[13.5px] text-ink/55 sm:absolute sm:left-0 sm:order-none">
                Sayfa {veri.sayfa + 1} / {toplamSayfa}
              </span>
              <div className="order-1 flex gap-1.5 sm:order-none">
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
              <span className="order-3 text-[13.5px] text-ink/55 sm:absolute sm:right-0 sm:order-none">
                Toplam öğrenci sayısı : {veri.toplamKayit}
              </span>
            </div>
          </>
        )}
      </Card>
    </div>
  );
}
