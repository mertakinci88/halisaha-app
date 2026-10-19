import { Bar, BarChart, CartesianGrid, Tooltip, XAxis, YAxis } from 'recharts';
import { raporService } from '@/services';
import { Table, Th, Td, Tr, Bos } from '@/components/ui/table';
import { AYLAR, kisaTarih, tl } from '@/lib/format';
import { RENK, tlKisa, yuzde } from '@/lib/rapor';
import {
  Aciklama, GrafikAlani, Kutu, OranSatiri, Ozet, OzetIzgara, RaporDurumu, TooltipKutusu, useRapor,
} from './RaporBilesenleri';

/** Birden fazla aya yayılan dönem (ör. Bu yıl) için ay ay öğrenci ödeme tahsilatı. */
export default function OgrenciAylikGorunum({ baslangic, bitis }) {
  const rapor = useRapor(() => raporService.ogrenciAylik({ baslangic, bitis }), [baslangic, bitis]);

  return (
    <RaporDurumu rapor={rapor}>
      {(v) => {
        // Kayıtlı öğrencisi olmayan ayın oranı anlamsız olduğundan boş (—) bırakılır
        const aylar = v.aylar.map((a) => ({
          ...a,
          etiket: AYLAR[a.ay - 1].slice(0, 3) + ' ' + String(a.yil).slice(2),
          oranDeger: a.ogrenci > 0 ? Number(a.oran) : null,
        }));
        return (
          <>
            <Aciklama>
              {kisaTarih(v.baslangic)} – {kisaTarih(v.bitis)} arası öğrenci ödeme durumu, ödemenin ait olduğu aya göre ay ay gösterilir.
            </Aciklama>
            <OzetIzgara>
              <Ozet
                vurgu
                etiket="Toplam tahsilat"
                deger={tl(v.toplamTutar)}
                alt={'Aylık ortalama ' + tl(v.aylikOrtalamaTutar)}
                bilgi="Seçilen dönemde öğrencilerden alınan toplam ödeme. Her ödeme, ait olduğu aya yazılır."
              />
              <Ozet
                etiket="Ortalama tahsilat oranı"
                deger={yuzde(v.ortalamaOran)}
                bilgi="Her ay için aktif öğrencilerin yüzde kaçının o ayki ödemesini tamamladığına bakılır; bu oranların ortalamasıdır. Kayıtlı öğrencisi olmayan aylar hesaba katılmaz."
              />
              <Ozet
                etiket="Aktif öğrenci"
                deger={v.aktifOgrenci}
                bilgi="Şu an aktif olan öğrenci sayısı."
              />
              <Ozet
                etiket="Yeni kayıt"
                deger={v.yeniKayit}
                bilgi="Seçilen dönemde kaydı yapılan öğrenci sayısı."
              />
            </OzetIzgara>

            <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
              <Kutu baslik="Aylık tahsilat" bilgi="Her ayın tahsilatı. Çubuğun üzerine gelince o ay ödeyen öğrenci sayısını ve oranı görebilirsiniz.">
                <GrafikAlani>
                  <BarChart data={aylar} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
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
                              { ad: 'Ödeme oranı', deger: payload[0].payload.oranDeger === null ? '—' : yuzde(payload[0].payload.oran) },
                            ]}
                          />
                        ) : null
                      }
                    />
                    <Bar dataKey="tutar" fill={RENK.ogrenci} isAnimationActive={false} />
                  </BarChart>
                </GrafikAlani>
              </Kutu>

              <Kutu baslik="Aylık tahsilat oranı" bilgi="Her ay aktif öğrencilerin yüzde kaçının ödemesini tamamladığı. O aya ait kayıtlı öğrenci yoksa çubuk gösterilmez.">
                <GrafikAlani>
                  <BarChart data={aylar} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                    <CartesianGrid stroke={RENK.izgara} vertical={false} />
                    <XAxis dataKey="etiket" tick={{ fontSize: 11, fill: RENK.yazi }} interval="preserveStartEnd" minTickGap={14} />
                    <YAxis domain={[0, 100]} tick={{ fontSize: 12, fill: RENK.yazi }} tickFormatter={(n) => '%' + n} width={44} />
                    <Tooltip
                      cursor={{ fill: 'rgba(29,31,32,0.06)' }}
                      content={({ active, payload }) =>
                        active && payload?.length ? (
                          <TooltipKutusu
                            baslik={payload[0].payload.etiket}
                            satirlar={[
                              { ad: 'Ödeme oranı', deger: payload[0].payload.oranDeger === null ? '—' : yuzde(payload[0].payload.oran), renk: RENK.rezervasyon },
                              { ad: 'Ödeyen / öğrenci', deger: payload[0].payload.odeyen + ' / ' + payload[0].payload.ogrenci },
                            ]}
                          />
                        ) : null
                      }
                    />
                    <Bar dataKey="oranDeger" fill={RENK.rezervasyon} isAnimationActive={false} />
                  </BarChart>
                </GrafikAlani>
              </Kutu>
            </div>

            <Kutu baslik="Aylık detay" bilgi="Her ay için kayıtlı aktif öğrenci sayısı, ödemesini tamamlayan (Ödeyen), kısmi ödeme yapıp tamamlamayan (Kısmi) ve hiç ödeme yapmayan öğrenciler ile alınan toplam tutar (kısmi ödemeler dahil).">
              <Table kart>
                <thead>
                  <tr>
                    <Th>Ay</Th>
                    <Th className="text-right">Öğrenci</Th>
                    <Th className="text-right">Ödeyen</Th>
                    <Th className="text-right">Kısmi</Th>
                    <Th className="text-right">Hiç ödemeyen</Th>
                    <Th className="min-w-[140px]">Ödeme oranı</Th>
                    <Th className="text-right">Tahsilat</Th>
                  </tr>
                </thead>
                <tbody>
                  {aylar.length === 0 ? <Bos colSpan={7} /> : null}
                  {aylar.map((a) => (
                    <Tr key={a.yil + '-' + a.ay}>
                      <Td etiket="Ay">{AYLAR[a.ay - 1]} {a.yil}</Td>
                      <Td etiket="Öğrenci" className="text-right">{a.ogrenci || '—'}</Td>
                      <Td etiket="Ödeyen" className="text-right">{a.ogrenci ? a.odeyen : '—'}</Td>
                      <Td etiket="Kısmi" className="text-right">{a.ogrenci ? a.kismi : '—'}</Td>
                      <Td etiket="Hiç ödemeyen" className="text-right">{a.ogrenci ? a.odemeyen - a.kismi : '—'}</Td>
                      <Td etiket="Ödeme oranı">
                        {a.ogrenci ? <OranSatiri etiket={yuzde(a.oran)} deger="" oran={Number(a.oran)} renk={RENK.ogrenci} /> : '—'}
                      </Td>
                      <Td etiket="Tahsilat" className="text-right">{tl(a.tutar)}</Td>
                    </Tr>
                  ))}
                </tbody>
              </Table>
            </Kutu>

            <Kutu baslik="Grup bazında" bilgi="Her grubun dönemdeki toplam tahsilatı ve ayların ortalama ödeme oranı.">
              <Table kart>
                <thead>
                  <tr>
                    <Th>Grup</Th>
                    <Th className="text-right">Aktif öğrenci</Th>
                    <Th className="text-right">Toplam tahsilat</Th>
                    <Th className="min-w-[140px]">Ortalama ödeme oranı</Th>
                  </tr>
                </thead>
                <tbody>
                  {v.gruplar.length === 0 ? <Bos colSpan={4}>Grup bulunamadı.</Bos> : null}
                  {v.gruplar.map((g) => (
                    <Tr key={g.grupId ?? 'grupsuz'}>
                      <Td etiket="Grup">{g.grupAd}</Td>
                      <Td etiket="Aktif öğrenci" className="text-right">{g.aktif}</Td>
                      <Td etiket="Toplam tahsilat" className="text-right">{tl(g.toplamTutar)}</Td>
                      <Td etiket="Ortalama ödeme oranı">
                        <OranSatiri etiket={yuzde(g.ortalamaOran)} deger="" oran={Number(g.ortalamaOran)} renk={RENK.ogrenci} />
                      </Td>
                    </Tr>
                  ))}
                </tbody>
              </Table>
            </Kutu>
          </>
        );
      }}
    </RaporDurumu>
  );
}
