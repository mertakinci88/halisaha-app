import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Check, X } from 'lucide-react';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Tag } from '@/components/ui/tag';
import { Table, Th, Td, Tr } from '@/components/ui/table';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import { Input, Select } from '@/components/ui/input';
import IadeBolumu from '@/components/IadeBolumu';
import HesapSecimi, { HESAP_SECIM_UYARISI, HESAP_TANIMSIZ_UYARISI } from '@/components/HesapSecimi';
import { ConfirmDialog } from '@/components/ui/dialog';
import { rezervasyonService, urunService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata, useToast } from '@/context/ToastContext';
import { cn } from '@/lib/utils';
import { useAuth } from '@/context/AuthContext';
import {
  ODEME_DURUMU_LABEL,
  ODEME_TIPI_LABEL,
  ODEME_YONTEMI_LABEL,
  kisaTarih,
  saatOf,
  sureSaat,
  tarihSaat,
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
  const { yetkiVar } = useAuth();
  const [rez, setRez] = useState(null);
  const [urunler, setUrunler] = useState([]);
  const [hata, setHata] = useHata();
  const toast = useToast();
  const [yukleniyor, setYukleniyor] = useState(true);
  const [islemde, setIslemde] = useState(false);
  const [iptalOnay, setIptalOnay] = useState(false);
  const [odemeYontemi, setOdemeYontemi] = useState('');
  const [hesapId, setHesapId] = useState('');
  const [hesapSecenekSayisi, setHesapSecenekSayisi] = useState(0);
  const [kapora, setKapora] = useState('');
  const [kismiAcik, setKismiAcik] = useState(false);

  const yukle = useCallback(() => {
    setYukleniyor(true);
    setHata('');
    Promise.all([rezervasyonService.get(id), urunService.list()])
      .then(([r, u]) => {
        setRez(r);
        setUrunler(u);
      })
      .catch((e) => setHata(apiHata(e), { sessiz: true }))
      .finally(() => setYukleniyor(false));
  }, [id]);

  useEffect(() => {
    yukle();
  }, [yukle]);

  // Ekleme/çıkarma stoğu değiştirir; stok sütunu güncel kalsın (hata olsa da, başka cihaz satmış olabilir)
  const urunleriYenile = () =>
    urunService
      .list()
      .then(setUrunler)
      .catch(() => {});

  const urunEkle = async (urunId) => {
    setHata('');
    setIslemde(true);
    try {
      setRez(await rezervasyonService.urunEkle(id, urunId, 1));
      toast.basari((urunler.find((u) => u.id === urunId)?.ad || 'Ürün') + ' eklendi.');
    } catch (e) {
      setHata(apiHata(e));
    } finally {
      await urunleriYenile();
      setIslemde(false);
    }
  };

  const urunCikar = async (rezervasyonUrunId) => {
    setHata('');
    setIslemde(true);
    try {
      setRez(await rezervasyonService.urunCikar(id, rezervasyonUrunId));
      toast.basari('Ürün çıkarıldı.');
    } catch (e) {
      setHata(apiHata(e));
    } finally {
      await urunleriYenile();
      setIslemde(false);
    }
  };

  const iptalEt = async () => {
    setIptalOnay(false);
    setIslemde(true);
    try {
      await rezervasyonService.iptal(id);
      toast.basari('Rezervasyon iptal edildi.');
      navigate('/takvim');
    } catch (e) {
      setHata(apiHata(e));
    } finally {
      setIslemde(false);
    }
  };

  const odemeAl = async (tip, tutar) => {
    if (!odemeYontemi) {
      setHata('Lütfen ödeme yöntemini seçiniz');
      return;
    }
    if (odemeYontemi === 'HAVALE_EFT' && !hesapId) {
      setHata(hesapSecenekSayisi > 0 ? HESAP_SECIM_UYARISI : HESAP_TANIMSIZ_UYARISI);
      return;
    }
    setHata('');
    setIslemde(true);
    try {
      setRez(await rezervasyonService.odemeEkle(id, {
          tip,
          tutar,
          yontem: odemeYontemi,
          hesapId: odemeYontemi === 'HAVALE_EFT' && hesapId ? Number(hesapId) : null,
        }));
      if (tip !== 'ODEME') {
        setKapora('');
        setKismiAcik(false);
      }
      // Her ödemede yöntem yeniden bilinçli seçilsin
      setOdemeYontemi('');
      setHesapId('');
      toast.basari((ODEME_TIPI_LABEL[tip] || 'Ödeme') + ' alındı.');
    } catch (e) {
      setHata(apiHata(e));
    } finally {
      setIslemde(false);
    }
  };

  // Kapora yalnızca rezervasyon kaydı sırasında alınır; kayıttan sonra alınan ödemeler kısmi ödemedir
  const kismiTip = 'KISMI';
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
    { etiket: 'Ödeme durumu', deger: rez.abonelikId && Number(rez.odenecekTutar) === 0 ? 'Abonelik kapsamında' : ODEME_DURUMU_LABEL[rez.odemeDurumu] || '—' },
    { etiket: 'Kayıt tarihi', deger: tarihSaat(rez.kayitTarih) },
    { etiket: 'Kaydeden', deger: rez.kaydedenAd || '—' },
  ];

  return (
    <div>
      <PageHeader kicker="Rezervasyon" baslik="Rezervasyon detayı" />
      <Hata mesaj={hata} />
      {rez.abonelikId ? (
        <div className="mb-5 flex flex-wrap items-center gap-x-3 gap-y-1 border border-basari-300 bg-basari-100 px-4 py-2.5 text-[14.5px] text-basari-800">
          <b>Abone rezervasyonu</b>
          <span>Bu rezervasyon, haftalık sabit rezervasyon (abonelik) kapsamındadır; saha ücreti abonelik ödemesiyle alınır.</span>
          {yetkiVar('ABONELIK') ? (
            <Link to={'/aboneler/' + rez.abonelikId} className="ml-auto text-basari-800 underline">Aboneliği aç</Link>
          ) : null}
        </div>
      ) : null}

      <div className="grid items-start gap-5 [grid-template-columns:repeat(auto-fit,minmax(min(300px,100%),1fr))]">
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
                        disabled={islemde || rez.durum !== 'AKTIF' || u.adet <= 0}
                        onClick={() => urunEkle(u.id)}
                      >
                        {u.adet <= 0 ? 'Tükendi' : '+ Ekle'}
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
                    disabled={islemde || !aktif}
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
                  etiket={(ODEME_TIPI_LABEL[o.tip] || o.tip) + ' · ' + (ODEME_YONTEMI_LABEL[o.yontem] || o.yontem) + (o.hesapAd ? ' (' + o.hesapAd + ')' : '')}
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

          {odemeler.length > 0 && (
            <IadeBolumu
              listeParams={{ rezervasyonId: rez.id }}
              kaynaklar={[{ id: rez.id, alan: 'rezervasyonId', etiket: 'Rezervasyon', odenen: Number(rez.odenenTutar) }]}
            />
          )}

          {aktif && kalan > 0 && (
            <div className="grid gap-2 border-t border-hairline pt-2.5">
              <Select
                value={odemeYontemi}
                onChange={(e) => {
                  setOdemeYontemi(e.target.value);
                  setHesapId('');
                }}
                aria-label="Ödeme yöntemi"
              >
                <option value="">Ödeme yöntemi seçiniz</option>
                <option value="NAKIT">Nakit</option>
                <option value="KART">Kart</option>
                <option value="HAVALE_EFT">Havale / EFT</option>
              </Select>
              {odemeYontemi === 'HAVALE_EFT' && (
                <HesapSecimi value={hesapId} onChange={setHesapId} onSecenekler={setHesapSecenekSayisi} />
              )}
              <label
                className={cn(
                  'relative inline-flex min-h-10 w-fit cursor-pointer items-center gap-2 border px-3 text-[15px]',
                  kismiAcik ? 'border-accent bg-accent-100' : 'border-divider bg-bg',
                )}
              >
                <input
                  type="checkbox"
                  className="peer sr-only"
                  checked={kismiAcik}
                  onChange={(e) => {
                    setKismiAcik(e.target.checked);
                    if (!e.target.checked) setKapora('');
                  }}
                />
                <span
                  className={cn(
                    'grid size-[18px] place-items-center border-[1.5px] border-ink/50 bg-bg text-white',
                    'peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-accent',
                    kismiAcik && 'border-accent bg-accent',
                  )}
                >
                  {kismiAcik ? <Check size={13} strokeWidth={3} /> : null}
                </span>
                <span>{kismiEtiket} al</span>
              </label>
              {kismiAcik && (
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
              )}
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
