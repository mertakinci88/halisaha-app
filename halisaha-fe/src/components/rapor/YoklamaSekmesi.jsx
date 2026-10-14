import { Bar, BarChart, CartesianGrid, Legend, Tooltip, XAxis, YAxis } from 'recharts';
import { raporService } from '@/services';
import { Table, Th, Td, Tr, Bos } from '@/components/ui/table';
import { Tag } from '@/components/ui/tag';
import { kisaTarih } from '@/lib/format';
import { GUN_KISA, RENK, yuzde } from '@/lib/rapor';
import {
  Aciklama, GrafikAlani, Kutu, OranSatiri, Ozet, OzetIzgara, RaporDurumu, TooltipKutusu, useRapor,
} from './RaporBilesenleri';

export default function YoklamaSekmesi({ baslangic, bitis }) {
  const rapor = useRapor(() => raporService.yoklama({ baslangic, bitis }), [baslangic, bitis]);

  return (
    <RaporDurumu rapor={rapor}>
      {(v) => {
        const gunler = v.gunler.map((g) => ({ ...g, etiket: GUN_KISA[g.gun - 1] }));
        return (
          <>
            <OzetIzgara>
              <Ozet vurgu etiket="Devam oranı" bilgi="Yoklamada gelen öğrencilerin, gelen ve gelmeyenlere oranı. İzinli öğrenciler hesaba katılmaz." deger={yuzde(v.devamOrani)} alt={v.yoklamaGunu + ' gün yoklama alındı'} />
              <Ozet etiket="Geldi" bilgi="Seçilen dönemde yoklamada “geldi” işaretlenen kayıt sayısı." deger={v.geldi} />
              <Ozet etiket="Gelmedi" bilgi="Seçilen dönemde yoklamada “gelmedi” işaretlenen kayıt sayısı." deger={v.gelmedi} />
              <Ozet etiket="İzinli" bilgi="Seçilen dönemde yoklamada “izinli” işaretlenen kayıt sayısı." deger={v.izinli} />
            </OzetIzgara>

            <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
              <Kutu baslik="Grup bazında devam oranı" bilgi="Her grubun devam oranı ve kaç öğrencinin geldiği, gelmediği.">
                <div className="flex flex-col gap-3">
                  {v.gruplar.length === 0 ? <Aciklama>Bu dönemde yoklama kaydı yok.</Aciklama> : null}
                  {v.gruplar.map((g) => (
                    <OranSatiri
                      key={g.grupId ?? 'grupsuz'}
                      etiket={g.grupAd}
                      deger={g.geldi + ' geldi · ' + g.gelmedi + ' gelmedi · ' + yuzde(g.devamOrani)}
                      oran={Number(g.devamOrani)}
                      renk={RENK.ogrenci}
                    />
                  ))}
                </div>
              </Kutu>

              <Kutu baslik="Haftanın günlerine göre" bilgi="Hangi günlerde kaç öğrencinin geldiğini, gelmediğini ve izinli olduğunu gösterir. Yoklama alınmayan günler görünmez.">
                <GrafikAlani>
                  <BarChart data={gunler} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                    <CartesianGrid stroke={RENK.izgara} vertical={false} />
                    <XAxis dataKey="etiket" tick={{ fontSize: 12, fill: RENK.yazi }} />
                    <YAxis allowDecimals={false} tick={{ fontSize: 12, fill: RENK.yazi }} width={36} />
                    <Tooltip
                      cursor={{ fill: 'rgba(29,31,32,0.06)' }}
                      content={({ active, payload }) =>
                        active && payload?.length ? (
                          <TooltipKutusu
                            baslik={payload[0].payload.etiket}
                            satirlar={[
                              { ad: 'Geldi', deger: payload[0].payload.geldi, renk: RENK.ogrenci },
                              { ad: 'Gelmedi', deger: payload[0].payload.gelmedi, renk: RENK.gelmedi },
                              { ad: 'İzinli', deger: payload[0].payload.izinli, renk: RENK.izinli },
                            ]}
                          />
                        ) : null
                      }
                    />
                    <Legend wrapperStyle={{ fontSize: 13 }} />
                    <Bar dataKey="geldi" name="Geldi" stackId="y" fill={RENK.ogrenci} isAnimationActive={false} />
                    <Bar dataKey="gelmedi" name="Gelmedi" stackId="y" fill={RENK.gelmedi} isAnimationActive={false} />
                    <Bar dataKey="izinli" name="İzinli" stackId="y" fill={RENK.izinli} isAnimationActive={false} />
                  </BarChart>
                </GrafikAlani>
              </Kutu>
            </div>

            <Kutu baslik="Gruplara göre yoklama alma" bilgi="Her grupta dönem içinde kaç gün yoklama alındığı ve son yoklamanın tarihi. Hiç yoklama alınmayan grup “Alınmadı” görünür.">
              <Table kart>
                <thead>
                  <tr>
                    <Th>Grup</Th>
                    <Th className="text-right">Aktif öğrenci</Th>
                    <Th className="text-right">Yoklama alınan gün</Th>
                    <Th>Son yoklama</Th>
                  </tr>
                </thead>
                <tbody>
                  {v.grupYoklamalari.length === 0 ? <Bos colSpan={4}>Grup bulunamadı.</Bos> : null}
                  {v.grupYoklamalari.map((g) => (
                    <Tr key={g.grupId}>
                      <Td etiket="Grup">{g.grupAd}</Td>
                      <Td etiket="Aktif öğrenci" className="text-right tabular-nums">{g.aktifOgrenci}</Td>
                      <Td etiket="Yoklama alınan gün" className="text-right tabular-nums">{g.yoklamaGunu}</Td>
                      <Td etiket="Son yoklama">{g.sonYoklama ? kisaTarih(g.sonYoklama) : <Tag tone="warning">Alınmadı</Tag>}</Td>
                    </Tr>
                  ))}
                </tbody>
              </Table>
            </Kutu>

            <Kutu baslik="En çok devamsızlık yapanlar" bilgi="Seçilen dönemde en çok derse gelmeyen aktif öğrenciler. Velilerle görüşmek için kullanabilirsiniz." aciklama="Aktif öğrenciler, en fazla 10 kişi">
              <Table kart>
                <thead>
                  <tr>
                    <Th>Öğrenci</Th>
                    <Th>Grup</Th>
                    <Th className="text-right">Gelmedi</Th>
                    <Th className="text-right">Yoklama</Th>
                    <Th className="text-right">Devam</Th>
                  </tr>
                </thead>
                <tbody>
                  {v.devamsizlar.length === 0 ? <Bos colSpan={5}>Bu dönemde devamsızlık yok.</Bos> : null}
                  {v.devamsizlar.map((o) => (
                    <Tr key={o.ogrenciId}>
                      <Td etiket="Öğrenci">{o.adSoyad}</Td>
                      <Td etiket="Grup">{o.grupAd || '—'}</Td>
                      <Td etiket="Gelmedi" className="text-right">{o.gelmedi}</Td>
                      <Td etiket="Yoklama" className="text-right">{o.toplam}</Td>
                      <Td etiket="Devam" className="text-right">{yuzde(o.devamOrani)}</Td>
                    </Tr>
                  ))}
                </tbody>
              </Table>
            </Kutu>
            <Aciklama>Devam oranı = Geldi / (Geldi + Gelmedi). İzinli kayıtlar orana katılmaz.</Aciklama>
          </>
        );
      }}
    </RaporDurumu>
  );
}
