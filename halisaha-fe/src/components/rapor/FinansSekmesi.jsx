import { Bar, BarChart, CartesianGrid, Legend, Tooltip, XAxis, YAxis } from 'recharts';
import { raporService } from '@/services';
import { kisaTarih, AYLAR, tl } from '@/lib/format';
import { RENK, tlKisa, yuzdeFark } from '@/lib/rapor';
import {
  GrafikAlani, Kutu, OranSatiri, Ozet, OzetIzgara, RaporDurumu, TooltipKutusu, useRapor,
} from './RaporBilesenleri';

const KAYNAKLAR = [
  { alan: 'rezervasyon', ad: 'Rezervasyon', renk: RENK.rezervasyon },
  { alan: 'ogrenci', ad: 'Öğrenci ödemesi', renk: RENK.ogrenci },
  { alan: 'envanter', ad: 'Envanter satışı', renk: RENK.envanter },
  { alan: 'abonelik', ad: 'Abonelik', renk: RENK.abonelik },
];

const YONTEMLER = [
  { alan: 'nakit', ad: 'Nakit' },
  { alan: 'kart', ad: 'Kart' },
  { alan: 'havaleEft', ad: 'Havale / EFT' },
];

/** Bekleyen tutarı türlerine ayırıp alt alta gösterir; yalnızca bekleyeni olan türler listelenir. */
function BekleyenDagilimi({ bekleyen }) {
  const satirlar = [
    { ad: 'Rezervasyon', tutar: Number(bekleyen.rezervasyon) },
    { ad: 'Envanter satışı', tutar: Number(bekleyen.envanter) },
    { ad: 'Abonelik', tutar: Number(bekleyen.abonelik) },
  ].filter((s) => s.tutar > 0);
  if (satirlar.length === 0) return <span>Ödenmemiş tutar yok</span>;
  return (
    <div className="flex flex-col gap-0.5">
      {satirlar.map((s) => (
        <div key={s.ad} className="flex justify-between gap-3">
          <span>{s.ad}</span>
          <span className="tabular-nums text-ink/80">{tl(s.tutar)}</span>
        </div>
      ))}
    </div>
  );
}

export default function FinansSekmesi({ baslangic, bitis }) {
  const rapor = useRapor(() => raporService.finans({ baslangic, bitis }), [baslangic, bitis]);

  return (
    <RaporDurumu rapor={rapor}>
      {(v) => {
        const toplam = Number(v.tahsilat.toplam);
        const gunluk = v.birim === 'GUN';
        const veri = v.trend.map((n) => ({
          ...n,
          etiket: gunluk ? n.donem.slice(8, 10) + '.' + n.donem.slice(5, 7) : AYLAR[Number(n.donem.slice(5, 7)) - 1].slice(0, 3) + ' ' + n.donem.slice(2, 4),
        }));
        return (
          <>
            <OzetIzgara className="lg:grid-cols-2">
              <Ozet vurgu etiket="Tahsil edilen" bilgi="Seçilen dönemde kasaya giren toplam para: rezervasyon, öğrenci, envanter ve abonelik ödemeleri. Para hangi gün alındıysa o güne sayılır; iade edilen para, iadenin yapıldığı güne göre düşülmüştür (net tutar). Altındaki bilgi, karşılaştırma dönemindeki tahsilattır: seçtiğiniz dönemin hemen öncesindeki aynı uzunluktaki dönem (Bu yıl seçiliyse geçen yılın aynı günleri). O dönemde tahsilat varsa aradaki değişim yüzdesi de gösterilir." deger={tl(v.tahsilat.toplam)} fark={yuzdeFark(v.tahsilat.toplam, v.oncekiTahsilat.toplam)} alt={
                  <>
                    {Number(v.iade) > 0 ? <div>İade {tl(v.iade)} düşüldü</div> : null}
                    <div>Önceki dönem ({kisaTarih(v.oncekiBaslangic)} – {kisaTarih(v.oncekiBitis)}): {tl(v.oncekiTahsilat.toplam)}</div>
                  </>
                } />
              <Ozet etiket="Bekleyen" bilgi="Henüz ödenmemiş, alınması gereken tutar. Bu dönemdeki rezervasyonların (saha ve kafeterya ürünleri; maçı henüz oynanmamış olanlar dahil), bu dönemde satılan envanterin ve bu dönemde kaydı yapılan aboneliklerin kalan depozito borcudur. Öğrenci ödemeleri bu tutara dahil değildir, çünkü öğrencilerin ödemesi gereken sabit bir tutar tanımlı değil. Ödeme yapmayan öğrencileri Öğrenci sekmesinden görebilirsiniz." deger={tl(v.bekleyen.toplam)} alt={<BekleyenDagilimi bekleyen={v.bekleyen} />} />
            </OzetIzgara>
            <OzetIzgara className="lg:grid-cols-5">
              <Ozet etiket="Rezervasyon (saha + kafeterya)" bilgi="Rezervasyonlar için alınan tüm ödemeler: kapora, kısmi ve kalan ödemeler. Ödeme saha ücreti ve eklenen kafeterya ürünleri için tek seferde alındığı için ikisi bir arada gösterilir." deger={tl(v.tahsilat.rezervasyon)} />
              <Ozet etiket="Kafeterya satışı" deger={tl(v.kafeteryaSatisi)} alt="Yandaki rezervasyon tutarının içindedir" bilgi="Bu dönemdeki rezervasyonlara eklenen kafeterya ürünlerinin (su, içecek vb.) satış tutarı. Ödenip ödenmediğine bakılmaz. Rezervasyon ödemesi içinde zaten sayıldığı için bu tutar toplama ayrıca eklenmez; sadece kafeteryadan ne kadar satıldığını göstermek içindir."/>
              <Ozet etiket="Öğrenci ödemesi" bilgi="Öğrencilerin her ay yaptığı ödemeler. Ödemenin yapıldığı tarihe göre sayılır." deger={tl(v.tahsilat.ogrenci)} />
              <Ozet etiket="Abonelik" deger={tl(v.tahsilat.abonelik)} bilgi="Abonelik depozitosu olarak alınan ödemeler (kayıttaki ilk ödeme ve sonradan alınan kalan ödemeler). Ödemenin alındığı tarihe göre sayılır; iptal sonrası depozitodan oynanan haftalara ayrıca ödeme alınmadığı için tekrar sayılmaz; artan depozito iade edilirse iade gününde düşülür. Abonelerin her hafta ödediği maç ücretleri Rezervasyon satırına yazılır." />
              <Ozet etiket="Envanter satışı" bilgi="Öğrencilere satılan envanterler (forma, eşofman vb.) için alınan ödemeler. Satış başka bir ayda yapılıp ödeme bu dönemde alındıysa bu döneme yazılır." deger={tl(v.tahsilat.envanter)} />
            </OzetIzgara>

            <Kutu baslik="Tahsilat trendi" bilgi="Her gün (uzun dönemlerde her ay) kasaya giren para. Renkler paranın nereden geldiğini gösterir; çubuğun üzerine gelince tutarları görebilirsiniz." aciklama={gunluk ? 'Günlük tahsilat' : 'Aylık tahsilat'}>
              <GrafikAlani>
                <BarChart data={veri} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                  <CartesianGrid stroke={RENK.izgara} vertical={false} />
                  <XAxis dataKey="etiket" tick={{ fontSize: 12, fill: RENK.yazi }} interval="preserveStartEnd" minTickGap={18} />
                  <YAxis tick={{ fontSize: 12, fill: RENK.yazi }} tickFormatter={tlKisa} width={52} />
                  <Tooltip
                    cursor={{ fill: 'rgba(29,31,32,0.06)' }}
                    content={({ active, payload }) =>
                      active && payload?.length ? (
                        <TooltipKutusu
                          baslik={payload[0].payload.etiket}
                          satirlar={[
                            ...KAYNAKLAR.map((k) => ({ ad: k.ad, deger: tl(payload[0].payload[k.alan]), renk: k.renk })),
                            { ad: 'Toplam', deger: tl(payload[0].payload.toplam) },
                          ]}
                        />
                      ) : null
                    }
                  />
                  <Legend wrapperStyle={{ fontSize: 13 }} />
                  {KAYNAKLAR.map((k) => (
                    <Bar key={k.alan} dataKey={k.alan} name={k.ad} stackId="t" fill={k.renk} isAnimationActive={false} />
                  ))}
                </BarChart>
              </GrafikAlani>
            </Kutu>

            <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
              <Kutu baslik="Kaynağa göre" bilgi="Tahsilatın rezervasyon, öğrenci ödemesi ve envanter satışından ne kadarının geldiğini ve toplamdaki payını gösterir.">
                <div className="flex flex-col gap-3">
                  {KAYNAKLAR.map((k) => (
                    <OranSatiri
                      key={k.alan}
                      etiket={k.ad}
                      deger={tl(v.tahsilat[k.alan])}
                      oran={toplam ? (Number(v.tahsilat[k.alan]) / toplam) * 100 : 0}
                      renk={k.renk}
                    />
                  ))}
                </div>
              </Kutu>
              <Kutu baslik="Ödeme yöntemine göre" bilgi="Paranın nakit, kartla ya da havale/EFT ile ne kadar alındığı.">
                <div className="flex flex-col gap-3">
                  {YONTEMLER.map((y) => (
                    <OranSatiri key={y.alan} etiket={y.ad} deger={tl(v.yontem[y.alan])} oran={toplam ? (Number(v.yontem[y.alan]) / toplam) * 100 : 0} />
                  ))}
                </div>
              </Kutu>
            </div>
          </>
        );
      }}
    </RaporDurumu>
  );
}
