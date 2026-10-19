import { Link } from 'react-router-dom';
import { Bar, BarChart, CartesianGrid, Tooltip, XAxis, YAxis } from 'recharts';
import { raporService } from '@/services';
import { AYLAR, tl } from '@/lib/format';
import { RENK, tlKisa, yuzde } from '@/lib/rapor';
import OgrenciAylikGorunum from './OgrenciAylikGorunum';
import {
  Aciklama, GrafikAlani, Kutu, OranSatiri, Ozet, OzetIzgara, RaporDurumu, TooltipKutusu, useRapor,
} from './RaporBilesenleri';

function AyGorunumu({ yil, ay }) {
  const rapor = useRapor(() => raporService.ogrenci({ yil, ay }), [yil, ay]);

  return (
    <RaporDurumu rapor={rapor}>
      {(v) => {
        const veri = v.trend.map((m) => ({ ...m, etiket: AYLAR[m.ay - 1].slice(0, 3) + ' ' + String(m.yil).slice(2) }));
        // Öğrenciler sayfasındaki ödeme filtreleri içinde bulunulan ayı kapsar; bağlantı yalnızca o ay için anlamlıdır
        const buAy = v.yil === new Date().getFullYear() && v.ay === new Date().getMonth() + 1;
        const listeBaglantisi = (odeme) =>
          buAy ? (
            <Link to={'/ogrenciler?odeme=' + odeme} className="text-accent-700 underline">
              Listeyi aç
            </Link>
          ) : undefined;
        return (
          <>
            <Aciklama>
              {AYLAR[v.ay - 1]} {v.yil} ayı ödeme durumu (bitiş tarihinin ayı esas alınır).
            </Aciklama>
            <OzetIzgara className="lg:grid-cols-5">
              <Ozet vurgu etiket="Tahsilat oranı" bilgi="Aktif öğrencilerin yüzde kaçının bu ayki ödemesini tamamladığı. Kısmi ödeme yapıp ayı tamamlamayanlar dahil değildir." deger={yuzde(v.tahsilatOrani)} alt={v.odeyen + ' / ' + v.aktifOgrenci + ' aktif öğrenci'} />
              <Ozet etiket="Ödeyen" bilgi="Bu ayki ödemesini tamamlamış (Tamamı ödendi işaretli ya da parasız kapatılmış) aktif öğrenci sayısı." deger={v.odeyen} alt={listeBaglantisi('odendi')} />
              <Ozet etiket="Kısmi ödeyen" bilgi="Bu ay ödeme almış ama ayı henüz tamamlamamış aktif öğrenci sayısı. Kalan ödemeleri beklenir." deger={v.kismiOdeyen} alt={listeBaglantisi('kismi')} />
              <Ozet etiket="Hiç ödemeyen" bilgi="Bu ay için hiç ödeme girilmemiş aktif öğrenci sayısı. Ödeyen + Kısmi ödeyen + Hiç ödemeyen, aktif öğrenci sayısını verir." deger={v.odemeyen - v.kismiOdeyen} alt={listeBaglantisi('hic')} />
              <Ozet etiket="Ardışık 2 ay ödemeyen" bilgi="Hem geçen ay hem bu ay ödemesini tamamlamamış aktif öğrenciler. Takip edilmesi gereken öğrencileri gösterir." deger={v.ardisikOdemeyen} alt={v.yeniKayit + ' yeni kayıt · ' + v.pasifOgrenci + ' pasif öğrenci'} />
            </OzetIzgara>

            <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
              <Kutu baslik="Grup bazında ödeme oranı" bilgi="Her grupta kaç aktif öğrenciden kaçının bu ayki ödemesini tamamladığı.">
                <div className="flex flex-col gap-3">
                  {v.gruplar.length === 0 ? <Aciklama>Aktif öğrenci yok.</Aciklama> : null}
                  {v.gruplar.map((g) => (
                    <OranSatiri
                      key={g.grupId ?? 'grupsuz'}
                      etiket={g.grupAd}
                      deger={g.odeyen + ' / ' + g.aktif + ' · ' + yuzde(g.oran)}
                      oran={Number(g.oran)}
                      renk={RENK.ogrenci}
                    />
                  ))}
                </div>
              </Kutu>

              <Kutu baslik="Aylık tahsilat" bilgi="Son 12 ayda öğrencilerden alınan ödeme tutarı. Çubuğun üzerine gelince o ay ödemesini tamamlayan öğrenci sayısını da görebilirsiniz." aciklama="Son 12 ay, tahsil edilen ödemeler">
                <GrafikAlani>
                  <BarChart data={veri} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                    <CartesianGrid stroke={RENK.izgara} vertical={false} />
                    <XAxis dataKey="etiket" tick={{ fontSize: 11, fill: RENK.yazi }} interval="preserveStartEnd" minTickGap={14} />
                    <YAxis tick={{ fontSize: 12, fill: RENK.yazi }} tickFormatter={tlKisa} width={52} />
                    <Tooltip
                      cursor={{ fill: 'rgba(29,31,32,0.06)' }}
                      content={({ active, payload }) =>
                        active && payload?.length ? (
                          <TooltipKutusu
                            baslik={payload[0].payload.etiket}
                            satirlar={[
                              { ad: 'Tahsilat', deger: tl(payload[0].payload.tutar), renk: RENK.ogrenci },
                              { ad: 'Ödeyen öğrenci', deger: payload[0].payload.odeyen },
                            ]}
                          />
                        ) : null
                      }
                    />
                    <Bar dataKey="tutar" fill={RENK.ogrenci} isAnimationActive={false} />
                  </BarChart>
                </GrafikAlani>
              </Kutu>
            </div>
          </>
        );
      }}
    </RaporDurumu>
  );
}

/** Seçilen tarih aralığı tek bir aydaysa o ayın görünümü, birden fazla aya yayılıyorsa ay ay görünüm. */
export default function OgrenciSekmesi({ baslangic, bitis }) {
  const tekAy = baslangic.slice(0, 7) === bitis.slice(0, 7);
  return tekAy ? (
    <AyGorunumu yil={Number(bitis.slice(0, 4))} ay={Number(bitis.slice(5, 7))} />
  ) : (
    <OgrenciAylikGorunum baslangic={baslangic} bitis={bitis} />
  );
}
