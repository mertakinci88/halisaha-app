import { useEffect, useRef, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { Card } from '@/components/ui/card';
import { Tag } from '@/components/ui/tag';
import { Table, Th, Td, Tr, Bos } from '@/components/ui/table';
import { Yukleniyor } from '@/components/ui/durum';
import { ogrenciYoklamaService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata } from '@/context/ToastContext';
import { Sayfalama } from '@/components/rapor/RaporBilesenleri';
import { GUNLER, kisaTarih } from '@/lib/format';

const DURUM = {
  GELDI: { ad: 'Geldi', sinif: 'bg-basari-100 text-basari-800' },
  GELMEDI: { ad: 'Gelmedi', sinif: 'bg-hata-100 text-hata-800' },
  IZINLI: { ad: 'İzinli', sinif: 'bg-accent-100 text-accent-800' },
};

const BOYUT = 20;
/** Yoklama sayfasından gelen bağlantı bu hash ile doğrudan karta kaydırır. */
export const YOKLAMA_KARTI_HASH = '#yoklama-bilgileri';

const gunAdi = (iso) =>
  GUNLER[new Date(String(iso).slice(0, 10) + 'T00:00:00').getDay()];

/** Öğrenci detayındaki yoklama bilgileri: öğrencinin alınmış yoklamaları tarih bazlı (en yeni üstte) listelenir. */
export default function OgrenciYoklamaKarti({ ogrenciId }) {
  const [hata, setHata] = useHata();
  const [sayfa, setSayfa] = useState(0);
  const [veri, setVeri] = useState(null);
  const [yukleniyor, setYukleniyor] = useState(true);

  const { hash } = useLocation();
  const kaydirildi = useRef(false);

  useEffect(() => setSayfa(0), [ogrenciId]);

  // Üstteki kartlar (transfer, ödeme) sonradan yüklenip sayfayı kaydırabildiği için kısa süre sonra bir kez daha kaydırılır
  useEffect(() => {
    if (hash !== YOKLAMA_KARTI_HASH || !veri || kaydirildi.current) return;
    kaydirildi.current = true;
    const git = () => {
      const kart = document.getElementById('yoklama-bilgileri');
      if (!kart) return;
      // Kart ekrandan uzunsa ortalamak başlığı keser; o zaman üste hizalanır
      kart.scrollIntoView({ block: kart.offsetHeight < window.innerHeight ? 'center' : 'start' });
    };
    git();
    const zamanlayici = setTimeout(git, 600);
    return () => clearTimeout(zamanlayici);
  }, [hash, veri]);

  useEffect(() => {
    setYukleniyor(true);
    ogrenciYoklamaService
      .gecmis(ogrenciId, { sayfa, boyut: BOYUT })
      .then(setVeri)
      .catch((e) => setHata(apiHata(e), { sessiz: true }))
      .finally(() => setYukleniyor(false));
  }, [ogrenciId, sayfa]);

  const liste = veri ? veri.sayfa.icerik : [];
  const toplam = veri ? veri.sayfa.toplamKayit : 0;
  const sayi = {
    GELDI: veri?.geldi,
    GELMEDI: veri?.gelmedi,
    IZINLI: veri?.izinli,
  };

  return (
    <Card id="yoklama-bilgileri" className="mt-5 scroll-mt-4 gap-4">
      <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
        <h4 className="text-[22px]">Yoklama bilgileri</h4>
        {!veri || toplam === 0 ? null : (
          <div className="flex flex-wrap gap-2">
            {Object.entries(DURUM).map(([kod, d]) => (
              <Tag key={kod} tone="neutral" className={d.sinif}>
                {d.ad} {sayi[kod]}
              </Tag>
            ))}
          </div>
        )}
      </div>

      {!veri && yukleniyor ? (
        <Yukleniyor />
      ) : hata ? (
        <div role="alert" className="text-[14px] text-hata-800">
          {hata}
        </div>
      ) : (
        <div>
          <Table kart>
            <thead>
              <tr>
                <Th>Tarih</Th>
                <Th>Gün</Th>
                <Th>Durum</Th>
                <Th>Not</Th>
                <Th>Kaydeden</Th>
              </tr>
            </thead>
            <tbody>
              {liste.length === 0 ? (
                <Bos colSpan={5}>Bu öğrenci için alınmış yoklama yok.</Bos>
              ) : (
                liste.map((y) => {
                  const d = DURUM[y.durum];
                  return (
                    <Tr key={y.tarih}>
                      <Td etiket="Tarih" className="whitespace-nowrap">
                        {kisaTarih(y.tarih)}
                      </Td>
                      <Td etiket="Gün">{gunAdi(y.tarih)}</Td>
                      <Td etiket="Durum">
                        <Tag tone="neutral" className={d.sinif}>
                          {d.ad}
                        </Tag>
                      </Td>
                      <Td etiket="Not">{y.not || '—'}</Td>
                      <Td etiket="Kaydeden">{y.kaydeden || '—'}</Td>
                    </Tr>
                  );
                })
              )}
            </tbody>
          </Table>
          <Sayfalama
            sayfa={sayfa}
            sayfaBoyutu={BOYUT}
            toplam={toplam}
            onSayfa={setSayfa}
            yukleniyor={yukleniyor}
          />
        </div>
      )}
    </Card>
  );
}
