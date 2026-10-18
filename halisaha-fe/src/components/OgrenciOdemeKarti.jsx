import { useEffect, useMemo, useState } from 'react';
import { ChevronLeft, ChevronRight, Save, X } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input, Select } from '@/components/ui/input';
import OdemeDurumuEtiketi from '@/components/OdemeDurumuEtiketi';
import IadeBolumu from '@/components/IadeBolumu';
import { Table, Th, Td, Tr } from '@/components/ui/table';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import { hesapService, ogrenciOdemeService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata, useToast } from '@/context/ToastContext';
import { AYLAR, toIsoDate, tl } from '@/lib/format';
import { cn } from '@/lib/utils';
import { useMedia } from '@/lib/useMedia';

const SATIR_RENK = {
  ODENDI: 'bg-basari-100 hover:bg-basari-100',
  ODEME_BEKLIYOR: 'bg-yellow-100 hover:bg-yellow-100',
  ODENMEDI: 'bg-hata-100 hover:bg-hata-100',
};

const taslak = (a) => ({
  tutar: Number(a.odenenTutar) > 0 ? String(a.odenenTutar) : '',
  sekil: a.odemeSekli || '',
  hesapId: a.hesapId != null ? String(a.hesapId) : '',
  tarih: a.odemeTarihi ? String(a.odemeTarihi).slice(0, 10) : '',
  not: a.not || '',
});

const tutarSayi = (t) => (t === '' ? 0 : Number(t));

/** Satırın kayıtlı haline göre değişip değişmediği. Tutar boşken (yalnızca not) şekil / tarih önemsiz. */
const degisti = (t, ilk) => {
  if (tutarSayi(t.tutar) !== tutarSayi(ilk.tutar) || t.not.trim() !== ilk.not.trim()) return true;
  if (tutarSayi(t.tutar) === 0) return false;
  return t.sekil !== ilk.sekil || (t.sekil === 'HAVALE_EFT' && t.hesapId !== ilk.hesapId) || t.tarih !== ilk.tarih;
};

/**
 * Öğrenci detayındaki aylık ödemeler: seçili yılın 12 ayı için ödenen tutar, ödeme yöntemi (Havale / EFT'de hesap),
 * ödeme tarihi ve not girilir. Sabit tutar ya da üst sınır yoktur; tutar kaydedilen ay Ödendi (yeşil); kaydedilmeyen ay ayın 15'ine kadar Ödeme bekliyor (sarı), sonra Ödenmedi (kırmızı). Renkler kaydedilmiş durumu gösterir.
 * Yalnızca değiştirilen aylar kaydedilir; tutarı boşaltılan ayın kaydı silinir.
 */
export default function OgrenciOdemeKarti({ ogrenciId }) {
  const toast = useToast();
  const [hata, setHata] = useHata();
  const mobil = useMedia('(max-width: 767px)');
  const [yil, setYil] = useState(new Date().getFullYear());

  const [veri, setVeri] = useState(null);
  const [taslaklar, setTaslaklar] = useState({});
  const [hesaplar, setHesaplar] = useState([]);
  const [yukleniyor, setYukleniyor] = useState(true);
  const [kaydediyor, setKaydediyor] = useState(false);
  const [satirHata, setSatirHata] = useState('');

  const uygula = (v) => {
    setVeri(v);
    setTaslaklar(Object.fromEntries(v.aylar.map((a) => [a.ay, taslak(a)])));
    setSatirHata('');
  };

  const yukle = () => {
    setYukleniyor(true);
    setHata('');
    ogrenciOdemeService
      .getir(ogrenciId, yil)
      .then(uygula)
      .catch((e) => setHata(apiHata(e), { sessiz: true }))
      .finally(() => setYukleniyor(false));
  };

  useEffect(yukle, [ogrenciId, yil]);

  useEffect(() => {
    hesapService.list().then(setHesaplar).catch(() => setHesaplar([]));
  }, []);

  const ilkler = useMemo(() => (veri ? Object.fromEntries(veri.aylar.map((a) => [a.ay, taslak(a)])) : {}), [veri]);
  const degisenler = veri ? veri.aylar.filter((a) => taslaklar[a.ay] && degisti(taslaklar[a.ay], ilkler[a.ay])) : [];

  const sat = (ay, kismi) => {
    setSatirHata('');
    setTaslaklar((p) => ({ ...p, [ay]: { ...p[ay], ...kismi } }));
  };

  const tutarYaz = (a, deger) => {
    const t = taslaklar[a.ay];
    // İlk tutar girilirken tarih bugüne dolar (backend de boş tarihi bugün sayar)
    sat(a.ay, { tutar: deger, tarih: t.tarih || (deger !== '' ? toIsoDate(new Date()) : '') });
  };

  const kaydet = async () => {
    for (const a of degisenler) {
      const t = taslaklar[a.ay];
      if (tutarSayi(t.tutar) < 0 || Number.isNaN(tutarSayi(t.tutar))) return setSatirHata(AYLAR[a.ay - 1] + ': geçerli bir tutar girin.');
      if (tutarSayi(t.tutar) > 0 && !t.sekil) {
        return setSatirHata(AYLAR[a.ay - 1] + ': ödeme yöntemini seçin.');
      }
      if (tutarSayi(t.tutar) > 0 && t.sekil === 'HAVALE_EFT' && !t.hesapId) {
        return setSatirHata(AYLAR[a.ay - 1] + ': Havale / EFT için hesap seçin.');
      }
    }
    setKaydediyor(true);
    try {
      const v = await ogrenciOdemeService.kaydet(ogrenciId, yil, {
        aylar: degisenler.map((a) => {
          const t = taslaklar[a.ay];
          const odendi = tutarSayi(t.tutar) > 0;
          return {
            ay: a.ay,
            odenenTutar: tutarSayi(t.tutar),
            odemeSekli: odendi ? t.sekil : null,
            hesapId: odendi && t.sekil === 'HAVALE_EFT' ? Number(t.hesapId) : null,
            odemeTarihi: odendi ? t.tarih || null : null,
            not: t.not.trim() || null,
          };
        }),
      });
      uygula(v);
      toast.basari('Aylık ödemeler kaydedildi.');
    } catch (err) {
      setHata(apiHata(err));
    } finally {
      setKaydediyor(false);
    }
  };

  const hesapSecenekleri = (secili) =>
    hesaplar.filter((h) => h.durum === 'AKTIF' || String(h.id) === String(secili));

  const yillar = veri?.yillar || [yil];
  const yilIndex = yillar.indexOf(yil);
  const yilGit = (y) => {
    if (degisenler.length > 0) return toast.hata('Önce değişiklikleri kaydedin.');
    setYil(y);
  };

  const girdiler = (a) => {
    const t = taslaklar[a.ay];
    const tutar = tutarSayi(t.tutar);
    return {
      tutar: (
        <div className="flex items-center gap-1.5">
          <Input
            type="number"
            min="0"
            step="50"
            inputMode="decimal"
            placeholder="0"
            aria-label={AYLAR[a.ay - 1] + ' ödenen tutar'}
            value={t.tutar}
            onChange={(e) => tutarYaz(a, e.target.value)}
            className="min-w-[96px] text-right tabular-nums"
          />
        </div>
      ),
      sekil: (
        <Select
          aria-label={AYLAR[a.ay - 1] + ' ödeme yöntemi'}
          disabled={tutar === 0}
          value={t.sekil}
          onChange={(e) => sat(a.ay, { sekil: e.target.value, hesapId: e.target.value === 'HAVALE_EFT' ? t.hesapId : '' })}
        >
          <option value="">Seçiniz</option>
          <option value="NAKIT">Nakit</option>
          <option value="KART">Kart</option>
          <option value="HAVALE_EFT">Havale / EFT</option>
        </Select>
      ),
      hesap:
        t.sekil === 'HAVALE_EFT' && tutar > 0 ? (
          <Select
            aria-label={AYLAR[a.ay - 1] + ' hesap'}
            value={t.hesapId}
            onChange={(e) => sat(a.ay, { hesapId: e.target.value })}
          >
            <option value="">Hesap seçin</option>
            {hesapSecenekleri(t.hesapId).map((h) => (
              <option key={h.id} value={h.id}>
                {h.ad}
                {h.durum === 'PASIF' ? ' (pasif)' : ''}
              </option>
            ))}
          </Select>
        ) : null,
      not: (
        <Input
          type="text"
          maxLength={255}
          placeholder="Not"
          aria-label={AYLAR[a.ay - 1] + ' not'}
          value={t.not}
          onChange={(e) => sat(a.ay, { not: e.target.value })}
        />
      ),
      tarih: (
        <Input
          type="date"
          aria-label={AYLAR[a.ay - 1] + ' ödeme tarihi'}
          disabled={tutar === 0}
          value={t.tarih}
          onChange={(e) => sat(a.ay, { tarih: e.target.value })}
        />
      ),
    };
  };

  /** Renk ve etiket her zaman son KAYDEDİLMİŞ durumu gösterir; girilen ama kaydedilmemiş değişiklik yalnızca "Değişti" işaretiyle belli olur. */
  const kayitliDurum = (a) => a.durum;
  const satirRenk = (a) => SATIR_RENK[kayitliDurum(a)];
  const durumTag = (a) => <OdemeDurumuEtiketi belirgin durum={kayitliDurum(a)} />;
  const degistiMi = (a) => degisenler.some((d) => d.ay === a.ay);
  // Kaydedilmemiş değişikliği olan satırı soldaki çubuk + "Değişti" etiketi ile işaretle
  // gizli: etiketin yeri her zaman ayrılır (görünmez), böylece Ay kolonu genişleyip tabloyu yana kaydırmaz
  const degistiEtiketi = (a, gizliYerTut = false) =>
    degistiMi(a) || gizliYerTut ? (
      <span
        aria-hidden={!degistiMi(a)}
        className={cn(
          'border border-accent bg-accent-100 px-1.5 text-[12px] font-semibold uppercase tracking-[0.06em] text-accent-800',
          !degistiMi(a) && 'invisible',
        )}
      >
        Değişti
      </span>
    ) : null;
  const buAyMi = (a) => veri != null && a.yil === Number(veri.bugun.slice(0, 4)) && a.ay === Number(veri.bugun.slice(5, 7));

  return (
    <Card className="mt-5 gap-3.5">
      <div className="flex flex-wrap items-center gap-3">
        <h4 className="text-[22px]">Aylık ödemeler</h4>
        <div className="ml-auto flex items-center gap-1">
          <Button size="icon" variant="secondary" aria-label="Önceki yıl" disabled={yukleniyor}
            onClick={() => yilGit(yilIndex > 0 ? yillar[yilIndex - 1] : yil - 1)}>
            <ChevronLeft size={18} />
          </Button>
          <Select aria-label="Yıl" className="w-auto" value={yil} disabled={yukleniyor}
            onChange={(e) => yilGit(Number(e.target.value))}>
            {yillar.map((y) => (
              <option key={y} value={y}>{y}</option>
            ))}
          </Select>
          <Button size="icon" variant="secondary" aria-label="Sonraki yıl" disabled={yukleniyor}
            onClick={() => yilGit(yilIndex >= 0 && yilIndex < yillar.length - 1 ? yillar[yilIndex + 1] : yil + 1)}>
            <ChevronRight size={18} />
          </Button>
        </div>
      </div>

      <Hata mesaj={hata} onTekrar={yukle} />
      {yukleniyor && !veri ? <Yukleniyor /> : null}

      {veri ? (
        <>
          <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1 border-l-4 border-accent bg-accent-100 px-3 py-2">
            <span className="text-[15.5px] font-semibold text-ink/80">{yil} toplam ödenen:</span>
            <b className="font-heading text-[26px] font-semibold tabular-nums text-accent-700">{tl(veri.toplamOdenen)}</b>
          </div>

          {mobil ? (
            <div className="flex flex-col">
              {veri.aylar.map((a) => {
                const g = girdiler(a);
                return (
                  <div key={a.ay} className={cn('grid gap-2 border-b border-l-4 border-b-hairline border-l-transparent px-2 py-3', satirRenk(a), degistiMi(a) && 'border-l-accent')}>
                    <div className="flex items-center gap-2">
                      <span className="font-semibold">{AYLAR[a.ay - 1]}</span>
                      {buAyMi(a) ? <span className="text-[12.5px] text-ink/60">bu ay</span> : null}
                      {degistiEtiketi(a)}
                      <span className="ml-auto">{durumTag(a)}</span>
                    </div>
                    {g.tutar}
                    <div className="grid grid-cols-2 gap-2">
                      {g.sekil}
                      {g.tarih}
                    </div>
                    {g.hesap}
                    {g.not}
                  </div>
                );
              })}
            </div>
          ) : (
            <Table>
              <thead>
                <tr>
                  <Th>Ay</Th>
                  <Th>Ödenen tutar</Th>
                  <Th>Ödeme yöntemi</Th>
                  <Th>Ödeme tarihi</Th>
                  <Th>Not</Th>
                  <Th>Durum</Th>
                </tr>
              </thead>
              <tbody>
                {veri.aylar.map((a) => {
                  const g = girdiler(a);
                  return (
                    <Tr key={a.ay} className={satirRenk(a)}>
                      <Td className={cn('whitespace-nowrap font-semibold', degistiMi(a) && 'shadow-[inset_4px_0_0_var(--color-accent)]')}>
                        {AYLAR[a.ay - 1]}
                        {buAyMi(a) ? <span className="ml-2 text-[12.5px] font-normal text-ink/60">bu ay</span> : null}
                        <span className="ml-2 align-middle">{degistiEtiketi(a, true)}</span>
                      </Td>
                      <Td>{g.tutar}</Td>
                      <Td>
                        <div className="grid gap-1.5">
                          {g.sekil}
                          {g.hesap}
                        </div>
                      </Td>
                      <Td>{g.tarih}</Td>
                      <Td className="min-w-[160px]">{g.not}</Td>
                      <Td>{durumTag(a)}</Td>
                    </Tr>
                  );
                })}
              </tbody>
            </Table>
          )}

          {satirHata ? (
            <div role="alert" className="border border-hata-600 bg-hata-100 px-3 py-2 text-[14px] text-hata-800">
              {satirHata}
            </div>
          ) : null}

          <div className="flex flex-wrap items-center gap-2">
            <Button variant="primary" disabled={kaydediyor || degisenler.length === 0} onClick={kaydet}>
              <Save size={16} strokeWidth={1.5} />
              <span>{kaydediyor ? 'Kaydediliyor…' : 'Ödemeleri kaydet'}</span>
            </Button>
            {degisenler.length > 0 ? (
              <>
                <Button variant="cerceveHata" disabled={kaydediyor} onClick={() => uygula(veri)}>
                  <X size={16} strokeWidth={1.5} />
                  <span>Vazgeç</span>
                </Button>
                <span className="text-[13.5px] text-ink/60">
                  {degisenler.length} ayda kaydedilmemiş değişiklik var:{' '}
                  <b className="font-semibold text-ink/80">{degisenler.map((a) => AYLAR[a.ay - 1]).join(', ')}</b>
                </span>
              </>
            ) : null}
          </div>

          <IadeBolumu
            listeParams={{ ogrenciId, yil }}
            kaynaklar={veri.aylar
              .filter((a) => a.id != null && Number(a.odenenTutar) > 0)
              .map((a) => ({ id: a.id, alan: 'ogrenciOdemeId', etiket: AYLAR[a.ay - 1] + ' ' + yil, odenen: Number(a.odenenTutar) }))}
          />
        </>
      ) : null}
    </Card>
  );
}
