import { Bar, BarChart, CartesianGrid, Tooltip, XAxis, YAxis } from 'recharts';
import { raporService } from '@/services';
import { Table, Th, Td, Tr, Bos } from '@/components/ui/table';
import { hh, tl } from '@/lib/format';
import { GUN_KISA, RENK, yuzde } from '@/lib/rapor';
import {
  Aciklama, GrafikAlani, Kutu, OranSatiri, Ozet, OzetIzgara, RaporDurumu, TooltipKutusu, useRapor,
} from './RaporBilesenleri';

export default function RezervasyonSekmesi({ baslangic, bitis }) {
  const rapor = useRapor(() => raporService.rezervasyon({ baslangic, bitis }), [baslangic, bitis]);

  return (
    <RaporDurumu rapor={rapor}>
      {(v) => {
        const saatler = v.saatler.map((s) => ({ ...s, etiket: hh(s.saat) }));
        const gunler = v.gunler.map((g) => ({ ...g, etiket: GUN_KISA[g.gun - 1] }));
        const tek = (ad) => ({ active, payload }) =>
          active && payload?.length ? (
            <TooltipKutusu baslik={payload[0].payload.etiket} satirlar={[{ ad, deger: payload[0].payload.adet, renk: RENK.rezervasyon }]} />
          ) : null;
        return (
          <>
            <OzetIzgara>
              <Ozet vurgu etiket="Doluluk" bilgi="Sahaların açık olduğu saatlerin yüzde kaçında rezervasyon olduğu. Sahalar her gün 12:00 – 02:00 arası (14 saat) açık kabul edilir." deger={yuzde(v.dolulukOrani)} alt={v.toplamSaat + ' saat rezerve'} />
              <Ozet etiket="Rezervasyon" bilgi="Seçilen dönemdeki iptal edilmemiş rezervasyon sayısı." deger={v.rezervasyon} />
              <Ozet etiket="İptal" bilgi="İptal edilen rezervasyon sayısı. Yüzde, tüm rezervasyonlara (iptaller dahil) oranıdır." deger={v.iptal} alt={yuzde(v.iptalOrani) + ' iptal oranı'} />
              <Ozet etiket="Saha geliri" bilgi="Rezervasyonlardaki saha kiralama ücretlerinin toplamı (kafeterya ürünleri hariç). İptaller dahil değildir; ödenip ödenmediğine bakılmaz." deger={tl(v.sahalar.reduce((t, s) => t + Number(s.sahaGeliri), 0))} />
            </OzetIzgara>

            <Kutu baslik="Saha bazında" bilgi="Her sahanın dönemdeki rezervasyon, iptal, saat ve gelir bilgisi. Doluluk çubuğu sahanın ne kadar dolu olduğunu gösterir." aciklama="Doluluk = rezerve saat / (gün sayısı x 14 çalışma saati)">
              <Table kart>
                <thead>
                  <tr>
                    <Th>Saha</Th>
                    <Th className="text-right">Rezervasyon</Th>
                    <Th className="text-right">İptal</Th>
                    <Th className="text-right">Saat</Th>
                    <Th className="text-right">Saha geliri</Th>
                    <Th className="min-w-[140px]">Doluluk</Th>
                  </tr>
                </thead>
                <tbody>
                  {v.sahalar.length === 0 ? <Bos colSpan={6}>Saha bulunamadı.</Bos> : null}
                  {v.sahalar.map((s) => (
                    <Tr key={s.sahaId}>
                      <Td etiket="Saha">{s.ad}</Td>
                      <Td etiket="Rezervasyon" className="text-right">{s.rezervasyon}</Td>
                      <Td etiket="İptal" className="text-right">{s.iptal}</Td>
                      <Td etiket="Saat" className="text-right">{s.saat}</Td>
                      <Td etiket="Saha geliri" className="text-right">{tl(s.sahaGeliri)}</Td>
                      <Td etiket="Doluluk">
                        <OranSatiri etiket={yuzde(s.dolulukOrani)} deger="" oran={Number(s.dolulukOrani)} renk={RENK.rezervasyon} />
                      </Td>
                    </Tr>
                  ))}
                </tbody>
              </Table>
            </Kutu>

            <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
              <Kutu baslik="Saatlere göre yoğunluk" bilgi="Rezervasyonların hangi saatte başladığı; en yoğun saatleri gösterir. 24 gece 00:00, 25 gece 01:00 demektir." aciklama="Başlangıç saatine göre rezervasyon sayısı">
                <GrafikAlani>
                  <BarChart data={saatler} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                    <CartesianGrid stroke={RENK.izgara} vertical={false} />
                    <XAxis dataKey="etiket" tick={{ fontSize: 11, fill: RENK.yazi }} interval="preserveStartEnd" minTickGap={10} />
                    <YAxis allowDecimals={false} tick={{ fontSize: 12, fill: RENK.yazi }} width={32} />
                    <Tooltip cursor={{ fill: 'rgba(29,31,32,0.06)' }} content={tek('Rezervasyon')} />
                    <Bar dataKey="adet" fill={RENK.rezervasyon} isAnimationActive={false} />
                  </BarChart>
                </GrafikAlani>
              </Kutu>
              <Kutu baslik="Günlere göre yoğunluk" bilgi="Haftanın hangi günlerinin daha yoğun olduğunu gösterir." aciklama="Haftanın gününe göre rezervasyon sayısı">
                <GrafikAlani>
                  <BarChart data={gunler} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                    <CartesianGrid stroke={RENK.izgara} vertical={false} />
                    <XAxis dataKey="etiket" tick={{ fontSize: 12, fill: RENK.yazi }} />
                    <YAxis allowDecimals={false} tick={{ fontSize: 12, fill: RENK.yazi }} width={32} />
                    <Tooltip cursor={{ fill: 'rgba(29,31,32,0.06)' }} content={tek('Rezervasyon')} />
                    <Bar dataKey="adet" fill={RENK.rezervasyon} isAnimationActive={false} />
                  </BarChart>
                </GrafikAlani>
              </Kutu>
            </div>
            <Aciklama>İptal edilen rezervasyonlar doluluğa ve gelire katılmaz. 00:00 ve 01:00 seansları, önceki günün devamı olarak sayılır.</Aciklama>
          </>
        );
      }}
    </RaporDurumu>
  );
}
