import { raporService } from '@/services';
import { Table, Th, Td, Tr, Bos } from '@/components/ui/table';
import { Tag } from '@/components/ui/tag';
import { tl } from '@/lib/format';
import { miktar, turBul } from '@/lib/envanter';
import { Aciklama, Kutu, Ozet, OzetIzgara, RaporDurumu, useRapor } from './RaporBilesenleri';

const urunAdi = (tur, beden) => (turBul(tur)?.ad || tur) + (beden === 'STANDART' ? '' : ' · ' + beden);

export default function EnvanterSekmesi({ baslangic, bitis }) {
  const rapor = useRapor(() => raporService.envanter({ baslangic, bitis }), [baslangic, bitis]);

  return (
    <RaporDurumu rapor={rapor}>
      {(v) => (
        <>
          <OzetIzgara>
            <Ozet vurgu etiket="Satış tutarı" bilgi="Bu dönemde öğrencilere satılan envanterlerin toplam fiyatı. Ödenip ödenmediğine bakılmaz." deger={tl(v.satisTutari)} alt={v.satisAdedi + ' satış'} />
            <Ozet etiket="Bu satışlar için ödenen" bilgi="Bu dönemde yapılan satışlar için şimdiye kadar alınan ödeme; sonradan alınan ödemeler de dahildir." deger={tl(v.tahsilEdilen)} />
            <Ozet etiket="Bekleyen" bilgi="Bu dönemde satılan envanterlerin henüz ödenmemiş kalan tutarı." deger={tl(v.bekleyen)} />
            <Ozet etiket="Kritik stok" bilgi="Stoğu uyarı sayısına eşit ya da altına düşen ürün sayısı." deger={v.kritikStok.length} alt="Uyarı eşiğinin altındaki kalem" />
          </OzetIzgara>

          <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
            <Kutu baslik="En çok satan ürünler" bilgi="Seçilen dönemde en çok satılan ürünler (tür ve bedene göre)." aciklama="Tür ve bedene göre, en fazla 10">
              <Table kart>
                <thead>
                  <tr>
                    <Th>Ürün</Th>
                    <Th className="text-right">Adet</Th>
                    <Th className="text-right">Tutar</Th>
                  </tr>
                </thead>
                <tbody>
                  {v.urunler.length === 0 ? <Bos colSpan={3}>Bu dönemde satış yok.</Bos> : null}
                  {v.urunler.map((u) => (
                    <Tr key={u.tur + u.beden}>
                      <Td etiket="Ürün">{urunAdi(u.tur, u.beden)}</Td>
                      <Td etiket="Adet" className="text-right">{miktar(u.tur, u.adet)}</Td>
                      <Td etiket="Tutar" className="text-right">{tl(u.tutar)}</Td>
                    </Tr>
                  ))}
                </tbody>
              </Table>
            </Kutu>

            <Kutu baslik="Set bazında satış" bilgi="Hangi setten kaç adet satıldığı. “Setsiz”, tek tek seçilerek yapılan satışlardır." aciklama="Setsiz satışlar ayrı gösterilir">
              <Table kart>
                <thead>
                  <tr>
                    <Th>Set</Th>
                    <Th className="text-right">Adet</Th>
                    <Th className="text-right">Tutar</Th>
                  </tr>
                </thead>
                <tbody>
                  {v.setler.length === 0 ? <Bos colSpan={3}>Bu dönemde satış yok.</Bos> : null}
                  {v.setler.map((s) => (
                    <Tr key={s.ad ?? 'setsiz'}>
                      <Td etiket="Set">{s.ad || 'Setsiz (tek tek)'}</Td>
                      <Td etiket="Adet" className="text-right">{s.adet}</Td>
                      <Td etiket="Tutar" className="text-right">{tl(s.tutar)}</Td>
                    </Tr>
                  ))}
                </tbody>
              </Table>
            </Kutu>
          </div>

          <Kutu baslik="Kritik stok" bilgi="Stoğu azalan ya da biten ürünler. Bugünkü durumu gösterir, tarih seçiminden etkilenmez." aciklama="Güncel durum; seçilen tarih aralığından etkilenmez">
            <Table kart>
              <thead>
                <tr>
                  <Th>Ürün</Th>
                  <Th className="text-right">Stok</Th>
                  <Th className="text-right">Uyarı eşiği</Th>
                  <Th>Durum</Th>
                </tr>
              </thead>
              <tbody>
                {v.kritikStok.length === 0 ? <Bos colSpan={4}>Stoğu azalan ürün yok.</Bos> : null}
                {v.kritikStok.map((s) => (
                  <Tr key={s.tur + s.beden}>
                    <Td etiket="Ürün">{urunAdi(s.tur, s.beden)}</Td>
                    <Td etiket="Stok" className="text-right">{miktar(s.tur, s.adet)}</Td>
                    <Td etiket="Uyarı eşiği" className="text-right">{s.uyariAdedi}</Td>
                    <Td etiket="Durum">
                      <Tag tone={s.adet === 0 ? 'danger' : 'warning'}>{s.adet === 0 ? 'Tükendi' : 'Azaldı'}</Tag>
                    </Td>
                  </Tr>
                ))}
              </tbody>
            </Table>
          </Kutu>
          <Aciklama>
            Satışlar kayıt tarihine göre sayılır; ödenen ve bekleyen tutar da bu satışlara aittir (sonradan alınan ödemeler dahil).
            Finans sekmesindeki tahsilat ise paranın alındığı tarihe göre hesaplanır.
          </Aciklama>
        </>
      )}
    </RaporDurumu>
  );
}
