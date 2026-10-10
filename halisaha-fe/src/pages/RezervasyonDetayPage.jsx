import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { X } from 'lucide-react';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Tag } from '@/components/ui/tag';
import { Table, Th, Td, Tr } from '@/components/ui/table';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import { Input, Select } from '@/components/ui/input';
import { ConfirmDialog } from '@/components/ui/dialog';
import { rezervasyonService, urunService } from '@/services';
import { apiHata } from '@/lib/api';
import { cn } from '@/lib/utils';
import {
  ODEME_DURUMU_LABEL,
  ODEME_TIPI_LABEL,
  ODEME_YONTEMI_LABEL,
  kisaTarih,
  saatOf,
  sureSaat,
  telGoster,
  tl,
  tutarSinirla,
} from '@/lib/format';

/** Ödeme özeti satırları ortak kolonları paylaşır: açıklama | tutar | eylem (tutarlar hizalı kalır). */
const OZET_IZGARA = 'grid items-center gap-x-3 [grid-template-columns:minmax(0,1fr)_auto_5.75rem]';

function OzetSatir({ etiket, alt, tutar, eylem, vurgu }) {
  return (
    <div className={OZET_IZGARA}>
      <span className={cn('min-w-0 text-[15.5px]', vurgu ? 'font-medium text-ink' : 'text-ink/55')}>
        {etiket}
        {alt && <span className="block text-[12px] opacity-80">{alt}</span>}
      </span>
      <span className={cn('text-right text-[15.5px] tabular-nums', vurgu && 'font-medium')}>{tutar}</span>
      <span className="flex justify-end">{eylem}</span>
    </div>
  );
}

export default function RezervasyonDetayPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [rez, setRez] = useState(null);
  const [urunler, setUrunler] = useState([]);
  const [hata, setHata] = useState('');
  const [yukleniyor, setYukleniyor] = useState(true);
  const [islemde, setIslemde] = useState(false);
  const [iptalOnay, setIptalOnay] = useState(false);
  const [odemeYontemi, setOdemeYontemi] = useState('NAKIT');
  const [kapora, setKapora] = useState('');

  const yukle = useCallback(() => {
    setYukleniyor(true);
    setHata('');
    Promise.all([rezervasyonService.get(id), urunService.list()])
      .then(([r, u]) => {
        setRez(r);
        setUrunler(u);
      })
      .catch((e) => setHata(apiHata(e)))
      .finally(() => setYukleniyor(false));
  }, [id]);

  useEffect(() => {
    yukle();
  }, [yukle]);

  const urunEkle = async (urunId) => {
    setIslemde(true);
    try {
      setRez(await rezervasyonService.urunEkle(id, urunId, 1));
    } catch (e) {
      setHata(apiHata(e));
    } finally {
      setIslemde(false);
    }
  };

  const urunCikar = async (rezervasyonUrunId) => {
    setIslemde(true);
    try {
      setRez(await rezervasyonService.urunCikar(id, rezervasyonUrunId));
    } catch (e) {
      setHata(apiHata(e));
    } finally {
      setIslemde(false);
    }
  };

  const iptalEt = async () => {
    setIptalOnay(false);
    setIslemde(true);
    try {
      await rezervasyonService.iptal(id);
      navigate('/takvim');
    } catch (e) {
      setHata(apiHata(e));
    } finally {
      setIslemde(false);
    }
  };

  const odemeAl = async (tip, tutar) => {
    setHata('');
    setIslemde(true);
    try {
      setRez(await rezervasyonService.odemeEkle(id, { tip, tutar, yontem: odemeYontemi }));
      if (tip !== 'ODEME') setKapora('');
    } catch (e) {
      setHata(apiHata(e));
    } finally {
      setIslemde(false);
    }
  };

  // Kapora rezervasyon başına bir kez alınır; sonrakiler kısmi ödeme olarak girilir
  const kaporaAlindi = (rez?.odemeler || []).some((o) => o.tip === 'KAPORA');
  const kismiTip = kaporaAlindi ? 'KISMI' : 'KAPORA';
  const kismiEtiket = ODEME_TIPI_LABEL[kismiTip];

  const kismiAl = () => {
    const tutar = Number(String(kapora).replace(',', '.'));
    if (!(tutar > 0)) {
      setHata(kismiEtiket + ' tutarı sıfırdan büyük olmalıdır.');
      return;
    }
    if (tutar > Number(rez.kalanTutar || 0)) {
      setHata(kismiEtiket + ' kalan tutarı (' + tl(rez.kalanTutar) + ') aşamaz.');
      return;
    }
    odemeAl(kismiTip, tutar);
  };

  if (yukleniyor) return <Yukleniyor />;
  if (!rez) return <Hata mesaj={hata || 'Rezervasyon bulunamadı.'} onTekrar={yukle} />;

  const sure = sureSaat(rez.baslangicTarih, rez.bitisTarih);
  const sahaTutar = Number(rez.sahaTutar || 0);
  const aktif = rez.durum === 'AKTIF';
  const kalan = Number(rez.kalanTutar || 0);
  const odemeler = rez.odemeler || [];

  // Aynı ürün (aynı birim fiyatla) tek satırda gösterilir: "Cips × 2". Çıkar, gruptaki son kaydı siler.
  const urunGruplari = Object.values(
    (rez.urunler || []).reduce((gruplar, u) => {
      const anahtar = u.urunId + '|' + u.birimFiyat;
      const g = (gruplar[anahtar] ||= { anahtar, urunAd: u.urunAd, miktar: 0, tutar: 0, satirlar: [] });
      g.miktar += Number(u.miktar || 0);
      g.tutar += Number(u.araToplam || 0);
      g.satirlar.push(u);
      return gruplar;
    }, {}),
  );

  const alanlar = [
    { etiket: 'Saha', deger: rez.sahaAd },
    { etiket: 'Tarih', deger: kisaTarih(rez.baslangicTarih) },
    { etiket: 'Saat', deger: saatOf(rez.baslangicTarih) + ' – ' + saatOf(rez.bitisTarih) },
    { etiket: 'Telefon', deger: telGoster(rez.telNo) },
    { etiket: 'Son ödeme yöntemi', deger: ODEME_YONTEMI_LABEL[rez.odemeYontemi] || '—' },
    { etiket: 'Ödeme durumu', deger: ODEME_DURUMU_LABEL[rez.odemeDurumu] || '—' },
  ];

  return (
    <div>
      <PageHeader kicker="Rezervasyon" baslik="Rezervasyon detayı" />
      <Hata mesaj={hata} />

      <div className="grid items-start gap-5 [grid-template-columns:repeat(auto-fit,minmax(300px,1fr))]">
        <div className="grid content-start gap-5">
          <Card>
            <div className="flex flex-wrap items-baseline gap-2.5">
              <h4 className="text-[22px]">{rez.adSoyad}</h4>
              <Tag tone={rez.durum === 'AKTIF' ? 'accent' : 'outline'}>
                {rez.durum === 'AKTIF' ? 'Aktif' : 'İptal'}
              </Tag>
            </div>
            <div className="grid gap-x-4 gap-y-3 [grid-template-columns:repeat(auto-fit,minmax(130px,1fr))]">
              {alanlar.map((a) => (
                <div key={a.etiket}>
                  <div className="text-[11.5px] uppercase tracking-[0.1em] text-ink/55">{a.etiket}</div>
                  <div className="text-[15.5px]">{a.deger}</div>
                </div>
              ))}
            </div>
          </Card>

          <Card>
            <h4 className="text-[22px]">Kafeterya · ürün ekle</h4>
            <p className="m-0 text-[13.5px] text-ink/55">
              {rez.durum === 'AKTIF'
                ? 'Seans sırasında satılan ürünleri ekleyin; tutar rezervasyona işlenir.'
                : 'İptal edilen rezervasyona ürün eklenemez.'}
            </p>
            <Table>
              <thead>
                <tr>
                  <Th>Ürün</Th>
                  <Th className="text-right">Fiyat</Th>
                  <Th className="text-right">Stok</Th>
                  <Th />
                </tr>
              </thead>
              <tbody>
                {urunler.map((u) => (
                  <Tr key={u.id}>
                    <Td>{u.ad}</Td>
                    <Td className="text-right tabular-nums">{tl(u.fiyat)}</Td>
                    <Td className="text-right tabular-nums text-ink/55">{u.adet}</Td>
                    <Td className="text-right">
                      <Button
                        variant="secondary"
                        size="sm"
                        disabled={islemde || rez.durum !== 'AKTIF'}
                        onClick={() => urunEkle(u.id)}
                      >
                        + Ekle
                      </Button>
                    </Td>
                  </Tr>
                ))}
              </tbody>
            </Table>
          </Card>
        </div>

        <Card className="gap-3">
          <h4 className="text-[22px]">Ödeme özeti</h4>
          <div className="grid gap-y-2">
            <OzetSatir etiket={rez.sahaAd + ' · ' + sure + ' saat'} tutar={tl(sahaTutar)} />

            {urunGruplari.map((g) => (
              <OzetSatir
                key={g.anahtar}
                etiket={g.urunAd + ' × ' + g.miktar}
                tutar={tl(g.tutar)}
                eylem={
                  <Button
                    variant="secondary"
                    size="sm"
                    disabled={islemde}
                    onClick={() => urunCikar(g.satirlar[g.satirlar.length - 1].id)}
                  >
                    <X size={14} strokeWidth={1.75} />
                    <span>Çıkar</span>
                  </Button>
                }
              />
            ))}
          </div>

          <div className="h-px bg-divider" />
          <div className={cn(OZET_IZGARA, 'items-baseline')}>
            <span className="font-heading text-[18px] font-semibold">Toplam</span>
            <span className="text-right font-heading text-[34px] font-semibold tabular-nums">
              {tl(rez.odenecekTutar)}
            </span>
            <span />
          </div>

          {odemeler.length > 0 && (
            <div className="grid gap-y-2 border-t border-hairline pt-2.5">
              {odemeler.map((o) => (
                <OzetSatir
                  key={o.id}
                  etiket={(ODEME_TIPI_LABEL[o.tip] || o.tip) + ' · ' + (ODEME_YONTEMI_LABEL[o.yontem] || o.yontem)}
                  alt={
                    kisaTarih(o.odemeTarihi) +
                    ' ' +
                    saatOf(o.odemeTarihi) +
                    (o.kullaniciAdSoyad ? ' · ' + o.kullaniciAdSoyad : '')
                  }
                  tutar={tl(o.tutar)}
                />
              ))}
              <div className="grid gap-y-2 border-t border-hairline pt-2">
                <OzetSatir etiket="Ödenen" tutar={tl(rez.odenenTutar)} />
                <OzetSatir etiket="Kalan" tutar={tl(kalan)} vurgu />
              </div>
            </div>
          )}

          {aktif && kalan > 0 && (
            <div className="grid gap-2 border-t border-hairline pt-2.5">
              <Select value={odemeYontemi} onChange={(e) => setOdemeYontemi(e.target.value)} aria-label="Ödeme yöntemi">
                <option value="NAKIT">Nakit</option>
                <option value="KART">Kart</option>
                <option value="HAVALE_EFT">Havale / EFT</option>
              </Select>
              <div className="grid gap-2 [grid-template-columns:1fr_auto]">
                <Input
                  type="number"
                  inputMode="decimal"
                  min="0"
                  max={kalan}
                  step="0.01"
                  placeholder={kismiEtiket + ' tutarı (₺)'}
                  value={kapora}
                  onChange={(e) => setKapora(tutarSinirla(e.target.value, kalan))}
                />
                <Button variant="secondary" disabled={islemde || !kapora} onClick={kismiAl}>
                  {kismiEtiket} al
                </Button>
              </div>
            </div>
          )}

          <div className="mt-1.5 grid gap-2">
            <Button
              variant="primary"
              disabled={islemde || kalan <= 0 || !aktif}
              onClick={() => odemeAl('ODEME', kalan)}
            >
              {rez.odemeDurumu === 'ODENDI'
                ? 'Ödeme alındı'
                : !aktif
                  ? 'İptal edildi · ödeme alınamaz'
                  : 'Ödemeyi tamamla · ' + tl(kalan)}
            </Button>
            <Button
              variant="secondary"
              disabled={islemde || rez.durum !== 'AKTIF'}
              onClick={() => setIptalOnay(true)}
            >
              Rezervasyonu iptal et
            </Button>
            <Button variant="ghost" onClick={() => navigate('/takvim')}>
              Takvime dön
            </Button>
          </div>
        </Card>
      </div>

      <ConfirmDialog
        open={iptalOnay}
        title="Rezervasyon iptal edilsin mi?"
        description="Bu işlem geri alınamaz."
        isim={rez.adSoyad}
        confirmLabel="İptal et"
        onConfirm={iptalEt}
        onCancel={() => setIptalOnay(false)}
      />
    </div>
  );
}
