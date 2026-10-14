import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Check, Lock, TriangleAlert } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input, Select } from '@/components/ui/input';
import { Tag } from '@/components/ui/tag';
import { Table, Th, Td, Tr, Bos } from '@/components/ui/table';
import { ConfirmDialog } from '@/components/ui/dialog';
import { Yukleniyor } from '@/components/ui/durum';
import HesapSecimi from '@/components/HesapSecimi';
import { envanterService, envanterSetService, envanterSatisService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata, useToast } from '@/context/ToastContext';
import { ODEME_YONTEMI_LABEL, stokDurumu, tarihSaat, tl } from '@/lib/format';
import { ENVANTER_TURLERI, STANDART } from '@/lib/envanter';
import { cn } from '@/lib/utils';
import { useMedia } from '@/lib/useMedia';

const TURLER = ENVANTER_TURLERI;

/** Satışın ödeme şekilleri ("Nakit, Kart"); hesapli: Havale / EFT'de hesap adı da yazılır. */
const yontemler = (satis, hesapli = false) => {
  const adlar = [
    ...new Set(
      satis.odemeler
        .filter((o) => o.odemeSekli)
        .map((o) => ODEME_YONTEMI_LABEL[o.odemeSekli] + (hesapli && o.hesapAd ? ' (' + o.hesapAd + ')' : '')),
    ),
  ];
  return adlar.length ? adlar.join(', ') : '—';
};

function Kutu({ on, pasif, onChange, renk, etiket, yazi }) {
  return (
    <label
      className={cn(
        'relative inline-flex min-h-10 min-w-10 cursor-pointer items-center justify-center',
        pasif && 'cursor-not-allowed opacity-35',
      )}
    >
      <input type="checkbox" className="peer sr-only" checked={on} disabled={pasif} onChange={onChange} aria-label={etiket} />
      <span
        className={cn(
          'grid size-[21px] place-items-center border-[1.5px] border-ink/50 bg-bg text-white',
          'peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-accent',
          on && renk,
        )}
      >
        {on ? <Check size={15} strokeWidth={3} /> : null}
      </span>
      {yazi ? <span className="ml-2 text-[15px]">{yazi}</span> : null}
    </label>
  );
}

const adBeden = (tur, beden) => (tur.bedenli ? tur.ad + ' (' + beden + ')' : tur.ad);

/**
 * Öğrenci detayındaki envanter seti satışı. Her envanter türünden bir kalem seçilir (bedenli türlerde beden);
 * her kalem için Ödendi ve Teslim ayrı işaretlenir. Ödendi işaretlenenlerin fiyatları ödenen tutara eklenir
 * (tutar elle de değiştirilebilir, set tutarını aşamaz). Satış fiyatı envanterden gelir ama satırda elle değiştirilebilir;
 * girilen fiyat o satış için geçerli olur. Stok yalnızca teslim edilen kalem için düşer; teslim
 * geri alınırsa geri döner. Stoğu biten envanter satılabilir ama teslim edilemez (Eksik envanter'de bekler).
 * Teslim edilmiş kalemin bedeni değiştirilemez.
 */
export default function EnvanterSatisKarti({ ogrenciId }) {
  const navigate = useNavigate();
  const toast = useToast();
  const [hata, setHata] = useHata();
  const kartRef = useRef(null);
  const mobil = useMedia('(max-width: 767px)');

  const [envanterler, setEnvanterler] = useState([]);
  const [setler, setSetler] = useState([]);
  const [setId, setSetId] = useState(''); // '' = özel (kayıtlı envanterlerden)
  const [satislar, setSatislar] = useState([]);
  const [yukleniyor, setYukleniyor] = useState(true);

  const [duzenlenen, setDuzenlenen] = useState(null); // güncellenen satış (null = yeni satış)
  const [secim, setSecim] = useState({}); // { TUR: beden }
  const [ekle, setEkle] = useState(() => new Set()); // satışa eklenen envanter türleri (Ekle işaretliler)
  const [odendi, setOdendi] = useState(() => new Set());
  const [teslim, setTeslim] = useState(() => new Set());
  const [odenen, setOdenen] = useState('');
  const [odemeSekli, setOdemeSekli] = useState('');
  const [hesapId, setHesapId] = useState('');
  const [hesapSayisi, setHesapSayisi] = useState(null); // seçilebilir Havale / EFT hesabı sayısı
  const [fiyatDuz, setFiyatDuz] = useState({}); // { TUR: '250' } elle değiştirilen satış fiyatları
  const [elle, setElle] = useState(false); // tutar elle değiştirildiyse Ödendi işaretleri üzerine yazmaz
  const [kaydediyor, setKaydediyor] = useState(false);
  const [kayitHatasi, setKayitHatasi] = useState(''); // yalnızca Satışı kaydet / güncelle hatası; kartın içinde gösterilir
  const [silinecek, setSilinecek] = useState(null);
  const [silinecekOdeme, setSilinecekOdeme] = useState(null); // silinmek istenen ödeme satırı

  const yukle = useCallback(() => {
    setHata('');
    return Promise.all([envanterService.list(), envanterSetService.list(), envanterSatisService.list(ogrenciId)])
      .then(([e, st, s]) => {
        setEnvanterler(e);
        setSetler(st);
        setSatislar(s);
      })
      .catch((err) => setHata(apiHata(err), { sessiz: true }))
      .finally(() => setYukleniyor(false));
  }, [ogrenciId]);

  useEffect(() => {
    yukle();
  }, [yukle]);

  /* ── seçime bağlı hesaplar ── */
  const bedenOf = (tur) => (tur.bedenli ? secim[tur.kod] : STANDART);
  const envOf = (tur) => {
    const b = bedenOf(tur);
    return b ? envanterler.find((e) => e.tur === tur.kod && e.beden === b) : undefined;
  };
  const orijinal = (tur) => duzenlenen?.kalemler.find((k) => k.tur === tur.kod);
  const kilitli = (tur) => Boolean(orijinal(tur)?.teslimEdildi);
  const zatenTeslim = (tur, env) => {
    const o = orijinal(tur);
    return Boolean(o?.teslimEdildi && env && o.envanterId === env.id);
  };
  // Elle girilen fiyat geçerlidir; yoksa satış anındaki fiyat korunur, beden değiştiyse yeni bedenin güncel fiyatı
  const fiyatOf = (tur) => {
    const e = envOf(tur);
    if (!e) return null;
    const duz = fiyatDuz[tur.kod];
    if (duz !== undefined) return duz === '' ? 0 : Number(duz);
    const o = orijinal(tur);
    return o && o.envanterId === e.id ? Number(o.birimFiyat) : Number(e.satisFiyati);
  };
  const teslimPasif = (tur) => {
    const e = envOf(tur);
    return !e || (e.adet <= 0 && !zatenTeslim(tur, e));
  };

  // Stok yenilenince stoğu biten envanterin (artık işaretlenemeyen) Teslim işareti kaldırılır
  useEffect(() => {
    setTeslim((p) => {
      const kalan = [...p].filter((kod) => !teslimPasif(TURLER.find((t) => t.kod === kod)));
      return kalan.length === p.size ? p : new Set(kalan);
    });
  }, [envanterler]);

  const hicEnvanterYok = envanterler.length === 0; // hiç envanter tanımlı değilse tablo gösterilmez
  // Özel satışta (ilk kayıt ve güncelleme) kaydı olan tüm envanterler listelenir, satışa "Ekle" işaretlenenler girer.
  // Set seçiliyse (ya da setli bir satış güncelleniyorsa) yalnızca o sete ait envanterler listelenir ve hepsi satışa girer.
  const seciliSet = setler.find((x) => String(x.id) === setId);
  const setliMod = duzenlenen ? Boolean(duzenlenen.setId) && setId !== 'silindi' : Boolean(seciliSet);
  const kayitliTurler = TURLER.filter((t) => envanterler.some((e) => e.tur === t.kod));
  const gosterilen = !setliMod
    ? kayitliTurler
    : duzenlenen
      ? kayitliTurler.filter((t) => duzenlenen.kalemler.some((k) => k.tur === t.kod))
      : kayitliTurler.filter((t) => seciliSet.kalemler.some((k) => k.tur === t.kod));
  const dahil = setliMod ? gosterilen : gosterilen.filter((t) => ekle.has(t.kod));
  const tutarEtiketi = setliMod ? 'Set tutarı' : 'Toplam tutar';
  const fiyatlar = dahil.map(fiyatOf);
  const hepsiSecili = fiyatlar.every((f) => f !== null);
  const toplam = fiyatlar.reduce((t, f) => t + (f || 0), 0);
  const odendiToplam = dahil.reduce((t, tur, i) => t + (odendi.has(tur.kod) ? fiyatlar[i] || 0 : 0), 0);

  // Ödendi işaretlenen envanterlerin fiyatları ödenen tutara otomatik yansır (tutar elle değiştirilene kadar)
  const odenenDeger = elle ? odenen : odendiToplam > 0 ? String(odendiToplam) : '';
  // Yeni satışta tüm ödeme, güncellemede mevcut ödemelere eklenen ek ödeme
  const yeniOdeme = Number(odenenDeger) || 0;
  const mevcutOdenen = duzenlenen ? Number(duzenlenen.odenenTutar) : 0;
  const tutar = mevcutOdenen + yeniOdeme;
  const asim = hepsiSecili && tutar > toplam;
  const durum = tutar <= 0 ? 'yok' : asim ? 'gecersiz' : tutar === toplam ? 'odendi' : 'kismi';
  const kalan = Math.max(toplam - tutar, 0);
  const stoksuzlar = dahil.filter((t) => {
    const e = envOf(t);
    return e && e.adet <= 0 && !zatenTeslim(t, e);
  });
  const hesapEksik = yeniOdeme > 0 && odemeSekli === 'HAVALE_EFT' && (!hesapId || hesapSayisi === 0);
  const kaydetPasif = dahil.length === 0 || !hepsiSecili || tutar <= 0 || asim || (yeniOdeme > 0 && !odemeSekli) || hesapEksik || kaydediyor;

  /* ── işlemler ── */
  const sifirla = () => {
    setKayitHatasi('');
    setSetId('');
    setDuzenlenen(null);
    setEkle(new Set());
    setSecim({});
    setOdendi(new Set());
    setTeslim(new Set());
    setOdenen('');
    setOdemeSekli('');
    setHesapId('');
    setElle(false);
    setFiyatDuz({});
  };

  // Set seçilince tablo setin envanterleriyle dolar (bedenler hazır gelir, satırlarda değiştirilebilir)
  const setSecildi = (id) => {
    setSetId(id);
    setKayitHatasi('');
    const st = setler.find((x) => String(x.id) === id);
    setEkle(new Set(st ? st.kalemler.map((k) => k.tur) : []));
    setSecim(st ? Object.fromEntries(st.kalemler.filter((k) => k.beden !== STANDART).map((k) => [k.tur, k.beden])) : {});
    setOdendi(new Set());
    setTeslim(new Set());
    setOdenen('');
    setElle(false);
    setFiyatDuz({});
  };

  const duzenlemeyeBasla = (satis) => {
    setSetId(satis.setId ? String(satis.setId) : satis.setAd ? 'silindi' : '');
    setDuzenlenen(satis);
    setKayitHatasi('');
    setEkle(new Set(satis.kalemler.map((k) => k.tur)));
    setSecim(Object.fromEntries(satis.kalemler.filter((k) => TURLER.find((t) => t.kod === k.tur)?.bedenli).map((k) => [k.tur, k.beden])));
    setOdendi(new Set(satis.kalemler.filter((k) => k.odendi).map((k) => k.tur)));
    setTeslim(new Set(satis.kalemler.filter((k) => k.teslimEdildi).map((k) => k.tur)));
    setOdenen(''); // güncellemede bu alan ek ödemedir; mevcut ödemeler listede görünür
    setOdemeSekli('');
    setHesapId('');
    setElle(true);
    setFiyatDuz({});
    kartRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  const ayarla = (kume, setKume, kod) => {
    const yeni = new Set(kume);
    if (yeni.has(kod)) yeni.delete(kod);
    else yeni.add(kod);
    setKume(yeni);
  };

  const ekleDegistir = (kod) => {
    ayarla(ekle, setEkle, kod);
    if (ekle.has(kod)) {
      // Satıştan çıkarılan envanterin Ödendi / Teslim işaretleri de temizlenir
      setOdendi((p) => new Set([...p].filter((k) => k !== kod)));
      setTeslim((p) => new Set([...p].filter((k) => k !== kod)));
      setFiyatDuz(({ [kod]: _, ...kalan }) => kalan);
      setElle(false);
    }
  };

  const odendiDegistir = (kod) => {
    ayarla(odendi, setOdendi, kod);
    setElle(false);
  };

  const bedenSec = (tur, beden) => {
    setSecim((p) => ({ ...p, [tur.kod]: beden }));
    setFiyatDuz(({ [tur.kod]: _, ...kalan }) => kalan); // beden değişince fiyat yeni bedenin fiyatına döner
    const yeni = envanterler.find((e) => e.tur === tur.kod && e.beden === beden);
    // Stoğu olmayan bedenin teslimi işaretlenemez
    if (!yeni || (yeni.adet <= 0 && !zatenTeslim(tur, yeni))) {
      setTeslim((p) => {
        const n = new Set(p);
        n.delete(tur.kod);
        return n;
      });
    }
  };

  const kaydet = async () => {
    setHata('');
    setKayitHatasi('');
    setKaydediyor(true);
    const govde = {
      kalemler: dahil.map((t) => ({
        tur: t.kod,
        beden: t.bedenli ? secim[t.kod] : null,
        odendi: odendi.has(t.kod),
        teslim: teslim.has(t.kod),
        birimFiyat: fiyatOf(t),
      })),
      odenenTutar: tutar,
      odemeSekli: yeniOdeme > 0 ? odemeSekli : null,
      hesapId: yeniOdeme > 0 && odemeSekli === 'HAVALE_EFT' ? Number(hesapId) : null,
      ...(duzenlenen || !seciliSet ? {} : { setId: seciliSet.id }),
    };
    try {
      if (duzenlenen) await envanterSatisService.update(ogrenciId, duzenlenen.id, govde);
      else await envanterSatisService.create(ogrenciId, govde);
      toast.basari(duzenlenen ? 'Envanter satışı güncellendi.' : 'Envanter satışı kaydedildi.');
      sifirla();
      await yukle();
    } catch (err) {
      setKayitHatasi(apiHata(err));
      yukle(); // stok vb. güncel kalsın
    } finally {
      setKaydediyor(false);
    }
  };

  const odemeSil = async () => {
    const o = silinecekOdeme;
    setSilinecekOdeme(null);
    try {
      const g = await envanterSatisService.odemeSil(ogrenciId, duzenlenen.id, o.id);
      setDuzenlenen(g);
      toast.basari('Ödeme silindi.');
      await yukle();
    } catch (err) {
      setKayitHatasi(apiHata(err));
    }
  };

  const sil = async () => {
    const s = silinecek;
    setSilinecek(null);
    try {
      await envanterSatisService.remove(ogrenciId, s.id);
      toast.basari('Envanter satışı silindi.');
      if (duzenlenen?.id === s.id) sifirla();
      await yukle();
    } catch (err) {
      setHata(apiHata(err));
    }
  };

  const bilgi = hicEnvanterYok
    ? { renk: 'text-yellow-800', metin: 'Henüz tanımlı envanter olmadığı için satış yapılamaz. Önce Envanter menüsünden envanter ekleyin.' }
    : dahil.length === 0
      ? { renk: 'text-yellow-800', metin: 'Önce satışa eklenecek envanterleri işaretleyin.' }
      : !hepsiSecili
      ? { renk: 'text-yellow-800', metin: 'Önce bedenli envanterlerin bedenini seçin.' }
      : yeniOdeme > 0 && !odemeSekli
        ? { renk: 'text-yellow-800', metin: 'Ödeme şeklini seçin.' }
        : stoksuzlar.length
          ? {
              renk: 'text-yellow-800',
              metin:
                'Stoğu biten envanter satılabilir (' +
                stoksuzlar.map((t) => adBeden(t, bedenOf(t))).join(', ') +
                '); stok girişi yapılana kadar teslim edilemez, “Eksik envanter”de bekler.',
            }
          : null;

  const satirlar = gosterilen.map((tur) => {
    const env = envOf(tur);
    const secenekler = envanterler.filter((e) => e.tur === tur.kod);
    const durumu = env ? stokDurumu(env) : null;
    const stokEtiketi =
      durumu === 'TUKENDI' ? (
        <Tag tone="danger" className="ml-2">Tükendi</Tag>
      ) : durumu === 'AZ' ? (
        <Tag tone="outline" className="ml-2">Stok az</Tag>
      ) : null;
    const bedenAlani = tur.bedenli ? (
      <div className="flex items-center gap-1.5">
        <Select
          value={secim[tur.kod] || ''}
          disabled={kilitli(tur) || secenekler.length === 0 || !ekle.has(tur.kod)}
          onChange={(e) => bedenSec(tur, e.target.value)}
          aria-label={tur.ad + ' bedeni'}
          className="min-h-9 w-[170px] py-1 text-[16px] disabled:bg-neutral-200 disabled:text-ink/60"
        >
          <option value="">{secenekler.length ? 'Beden seçin…' : 'Envanter yok'}</option>
          {secenekler.map((e) => (
            <option key={e.id} value={e.beden}>
              {e.beden} · {e.adet <= 0 ? 'Tükendi' : e.adet + ' adet'}
            </option>
          ))}
        </Select>
        {kilitli(tur) ? (
          <span title="Teslim edilen envanterin bedeni değiştirilemez">
            <Lock size={16} strokeWidth={1.5} className="text-ink/55" />
          </span>
        ) : null}
      </div>
    ) : (
      <span className="text-ink/55">{env ? 'Standart' : 'Envanter yok'}</span>
    );
    const dahilMi = setliMod || ekle.has(tur.kod);
    return { tur, env, fiyat: fiyatOf(tur), fiyatGirdi: fiyatDuz[tur.kod] ?? (fiyatOf(tur) === null ? '' : String(fiyatOf(tur))), stokEtiketi, bedenAlani, dahilMi, ekleKilitli: kilitli(tur) && teslim.has(tur.kod), teslimPasif: !dahilMi || teslimPasif(tur) };
  });

  const fiyatKutusu = (r) =>
    r.fiyat === null ? (
      <span className="text-ink/55">—</span>
    ) : (
      <Input
        type="number"
        min="0"
        step="1"
        inputMode="decimal"
        value={r.fiyatGirdi}
        disabled={!r.dahilMi}
        onChange={(e) => {
          const v = e.target.value;
          setFiyatDuz((p) => ({ ...p, [r.tur.kod]: v }));
        }}
        aria-label={r.tur.ad + ' satış fiyatı'}
        className="min-h-9 w-[110px] py-1 text-right text-[16px] tabular-nums disabled:bg-neutral-200 disabled:text-ink/60"
      />
    );

  if (yukleniyor) return <Yukleniyor />;

  return (
    <div ref={kartRef} className="mt-5 scroll-mt-4">
      <Card className="gap-4">
        <div className="flex flex-wrap items-end gap-3">
          <h4 className="text-[22px]">Envanter bilgileri</h4>
          {hicEnvanterYok ? null : (
            <div className="ml-auto text-right leading-none">
              <div className="text-[11.5px] uppercase tracking-[0.12em] text-ink/55">{tutarEtiketi}</div>
              <div className="font-heading text-[34px] font-semibold text-accent-700">{tl(toplam)}</div>
            </div>
          )}
        </div>

        {hata ? (
          <div className="flex items-start gap-2 border border-hata-600 bg-hata-100 px-3 py-2 text-[14.5px] text-hata-800">
            <TriangleAlert size={16} className="mt-0.5 shrink-0" />
            <span>{hata}</span>
          </div>
        ) : null}

        {hicEnvanterYok ? null : (
          <div className="max-w-[420px]">
            <label htmlFor="envanter-set" className="mb-1 block text-[13.5px] text-ink/70">Set</label>
            <Select
              id="envanter-set"
              value={setId}
              disabled={Boolean(duzenlenen)}
              onChange={(e) => setSecildi(e.target.value)}
              className="text-[16px] disabled:bg-neutral-200 disabled:text-ink/60"
            >
              <option value="">Özel (kayıtlı envanterlerden)</option>
              {duzenlenen && !duzenlenen.setId && duzenlenen.setAd ? <option value="silindi">{duzenlenen.setAd} (silinmiş set)</option> : null}
              {setler.map((x) => (
                <option key={x.id} value={String(x.id)}>
                  {x.ad} · {tl(x.toplamTutar)}
                </option>
              ))}
            </Select>
            <p className="m-0 mt-1 text-[13px] text-ink/55">
              {duzenlenen
                ? (setliMod
                  ? 'Satışın seti değiştirilemez; beden, Ödendi ve Teslim güncellenebilir.'
                  : 'Ekle ile envanter ekleyip çıkarabilir, beden, Ödendi ve Teslim güncelleyebilirsiniz.')
                : setler.length === 0
                  ? 'Henüz set tanımlı değil. Envanter menüsündeki Setler sekmesinden set ekleyebilirsiniz.'
                  : 'Set seçince envanterler tabloya dolar; bedenleri satırlardan değiştirebilirsiniz.'}
            </p>
          </div>
        )}

        <div className="grid grid-cols-[minmax(0,1fr)] items-start gap-5 lg:grid-cols-[minmax(0,1.55fr)_minmax(0,1fr)]">
          {hicEnvanterYok ? (
            <div className="flex flex-col items-start gap-2.5 border border-dashed border-ink/30 px-5 py-8">
              <div className="font-heading text-[22px] font-semibold text-ink/75">Henüz tanımlı envanter yok</div>
              <p className="m-0 text-[14.5px] text-ink/60">
                Öğrenciye envanter seti satabilmek için önce Envanter menüsünden envanterleri (forma, eşofman, tozluk vb.)
                stok ve satış fiyatıyla ekleyin. Eklendikten sonra bu bölümde set olarak listelenir.
              </p>
              <Button variant="secondary" size="sm" onClick={() => navigate('/envanter/yeni')}>
                Envanter ekle
              </Button>
            </div>
          ) : mobil ? (
            <div className="flex flex-col">
              {satirlar.map((r) => (
                <div key={r.tur.kod} className={cn('border-b border-hairline py-3', !r.dahilMi && 'opacity-60')}>
                  <div className="flex items-baseline justify-between gap-2">
                    <span className="font-semibold">{r.tur.ad}</span>
                    {fiyatKutusu(r)}
                  </div>
                  {setliMod ? null : (
                    <Kutu
                      on={r.dahilMi}
                      renk="border-accent bg-accent"
                      pasif={r.ekleKilitli}
                      onChange={() => ekleDegistir(r.tur.kod)}
                      etiket={r.tur.ad + ' satışa ekle'}
                      yazi="Satışa ekle"
                    />
                  )}
                  <div className="mt-2">{r.bedenAlani}</div>
                  <div className="mt-1.5 text-[14px] text-ink/70">
                    Stok: {r.env ? <>{r.env.adet}{r.stokEtiketi}</> : <span>—</span>}
                  </div>
                  <div className="mt-1 flex flex-wrap items-center gap-x-5">
                    <Kutu
                      on={odendi.has(r.tur.kod)}
                      pasif={!r.dahilMi}
                      renk="border-basari-600 bg-basari-600"
                      onChange={() => odendiDegistir(r.tur.kod)}
                      etiket={r.tur.ad + ' ödendi'}
                      yazi="Ödendi"
                    />
                    <Kutu
                      on={teslim.has(r.tur.kod)}
                      pasif={r.teslimPasif}
                      renk="border-accent bg-accent"
                      onChange={() => ayarla(teslim, setTeslim, r.tur.kod)}
                      etiket={r.tur.ad + ' teslim edildi'}
                      yazi="Teslim"
                    />
                  </div>
                </div>
              ))}
              <div className="flex items-baseline justify-between py-3 font-bold">
                <span>{tutarEtiketi}</span>
                <span className="text-[17px] tabular-nums">{tl(toplam)}</span>
              </div>
            </div>
          ) : (
            <Table>
              <thead>
                <tr>
                  {setliMod ? null : <Th className="text-center">Ekle</Th>}
                  <Th>Envanter</Th>
                  <Th>Beden</Th>
                  <Th className="text-right">Stok</Th>
                  <Th className="text-right">Satış fiyatı</Th>
                  <Th className="text-center">Ödendi</Th>
                  <Th className="text-center">Teslim</Th>
                </tr>
              </thead>
              <tbody>
                {satirlar.map((r) => (
                  <Tr key={r.tur.kod} className={cn(!r.dahilMi && 'opacity-60')}>
                    {setliMod ? null : (
                      <Td className="text-center">
                        <Kutu
                          on={r.dahilMi}
                          renk="border-accent bg-accent"
                          pasif={r.ekleKilitli}
                          onChange={() => ekleDegistir(r.tur.kod)}
                          etiket={r.tur.ad + ' satışa ekle'}
                        />
                      </Td>
                    )}
                    <Td className="whitespace-nowrap">{r.tur.ad}</Td>
                    <Td>{r.bedenAlani}</Td>
                    <Td className="whitespace-nowrap text-right tabular-nums">
                      {r.env ? <>{r.env.adet}{r.stokEtiketi}</> : <span className="text-ink/55">—</span>}
                    </Td>
                    <Td className="whitespace-nowrap text-right tabular-nums">
                      {fiyatKutusu(r)}
                    </Td>
                    <Td className="text-center">
                      <Kutu
                        on={odendi.has(r.tur.kod)}
                        pasif={!r.dahilMi}
                        renk="border-basari-600 bg-basari-600"
                        onChange={() => odendiDegistir(r.tur.kod)}
                        etiket={r.tur.ad + ' ödendi'}
                      />
                    </Td>
                    <Td className="text-center">
                      <Kutu
                        on={teslim.has(r.tur.kod)}
                        pasif={r.teslimPasif}
                        renk="border-accent bg-accent"
                        onChange={() => ayarla(teslim, setTeslim, r.tur.kod)}
                        etiket={r.tur.ad + ' teslim edildi'}
                      />
                    </Td>
                  </Tr>
                ))}
                <tr>
                  <Td colSpan={setliMod ? 3 : 4} className="font-bold">{tutarEtiketi}</Td>
                  <Td className="text-right text-[17px] font-bold tabular-nums">{tl(toplam)}</Td>
                  <Td />
                  <Td />
                </tr>
              </tbody>
            </Table>
          )}

          <div className="flex flex-col gap-3 border border-divider bg-surface/40 p-4">
            <div className="font-heading text-[20px] font-semibold">{duzenlenen ? 'Satışı güncelle' : 'Satış ve ödeme'}</div>

            {hicEnvanterYok ? null : (
              <>
              {duzenlenen ? (
                <div>
                  <div className="mb-1 text-[13.5px] text-ink/70">Yapılan ödemeler</div>
                  <div className="flex flex-col border border-divider bg-bg">
                    {duzenlenen.odemeler.map((o) => (
                      <div key={o.id} className="flex flex-wrap items-center gap-x-3 gap-y-1 border-b border-hairline px-3 py-2 text-[14px] last:border-b-0">
                        <span className="text-ink/60">{tarihSaat(o.odemeTarihi)}</span>
                        <b className="tabular-nums">{tl(o.tutar)}</b>
                        <span className="text-ink/70">
                          {o.odemeSekli ? ODEME_YONTEMI_LABEL[o.odemeSekli] + (o.hesapAd ? ' (' + o.hesapAd + ')' : '') : 'Şekil kaydı yok'}
                        </span>
                        <button
                          type="button"
                          disabled={duzenlenen.odemeler.length === 1}
                          onClick={() => setSilinecekOdeme(o)}
                          className="ml-auto cursor-pointer border-0 bg-transparent p-0 text-[13.5px] text-hata-800 underline disabled:cursor-not-allowed disabled:no-underline disabled:opacity-40"
                          title={duzenlenen.odemeler.length === 1 ? 'Satışın en az bir ödemesi olmalı' : undefined}
                        >
                          Sil
                        </button>
                      </div>
                    ))}
                  </div>
                </div>
              ) : null}
              <div>
                <label htmlFor="envanter-odenen" className="mb-1 block text-[13.5px] text-ink/70">{duzenlenen ? 'Ek ödeme (bu işlemde alınan)' : 'Ödenen tutar'}</label>
                <div className="relative">
                  <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ink/55">₺</span>
                  <Input
                    id="envanter-odenen"
                    type="number"
                    min="0"
                    step="1"
                    inputMode="numeric"
                    placeholder="0"
                    value={odenenDeger}
                    onChange={(e) => {
                      setOdenen(e.target.value);
                      setElle(true);
                    }}
                    className={cn('pl-7 text-[16px]', asim && 'border-hata-600 bg-hata-100')}
                  />
                </div>
                {asim ? (
                  <div className="mt-1.5 flex items-start gap-1.5 text-[13.5px] text-hata-800">
                    <TriangleAlert size={15} className="mt-0.5 shrink-0" />
                    <span>Ödenen tutar {setliMod ? 'set tutarını' : 'toplam tutarı'} ({tl(toplam)}) aşamaz.</span>
                  </div>
                ) : (
                  <p className="m-0 mt-1 text-[13px] text-ink/55">
                    {duzenlenen ? (
                      <>
                        Satışa ek olarak yeni bir ödeme aldıysanız tutarını girin; boş bırakırsanız ödeme eklenmez.
                        Şimdiye kadar ödenen {tl(mevcutOdenen)}, en fazla {tl(toplam)}.
                      </>
                    ) : (
                      <>
                        Tabloda <b>Ödendi</b> işaretlenen envanterlerin fiyatları otomatik toplanır; tutarı elle de
                        değiştirebilirsiniz. En fazla {tl(toplam)}.
                      </>
                    )}
                  </p>
                )}
              </div>

              {!duzenlenen || yeniOdeme > 0 ? (
              <div className="grid grid-cols-1 gap-3 min-[480px]:grid-cols-2">
                <div>
                  <label htmlFor="envanter-odeme-sekli" className="mb-1 block text-[13.5px] text-ink/70">Ödeme şekli</label>
                  <Select
                    id="envanter-odeme-sekli"
                    value={odemeSekli}
                    onChange={(e) => {
                      setOdemeSekli(e.target.value);
                      if (e.target.value !== 'HAVALE_EFT') setHesapId('');
                    }}
                  >
                    <option value="">Seçiniz</option>
                    {Object.entries(ODEME_YONTEMI_LABEL).map(([kod, ad]) => (
                      <option key={kod} value={kod}>{ad}</option>
                    ))}
                  </Select>
                </div>
                {odemeSekli === 'HAVALE_EFT' ? (
                  <HesapSecimi value={hesapId} onChange={setHesapId} onSecenekler={setHesapSayisi} />
                ) : null}
              </div>
              ) : null}

              <div className="text-[14px] text-ink/70">
                Ödeme durumu:{' '}
                {durum === 'yok' ? (
                  <Tag tone="neutral">Ödeme girilmedi</Tag>
                ) : durum === 'gecersiz' ? (
                  <Tag tone="danger">Geçersiz tutar</Tag>
                ) : durum === 'odendi' ? (
                  <Tag tone="neutral" className="bg-basari-100 text-basari-800">Ödendi</Tag>
                ) : (
                  <Tag tone="warning">Kısmi ödendi</Tag>
                )}
              </div>
              {tutar > 0 && hepsiSecili && !asim ? (
                <div className="text-[14px]">Kalan tutar: <b>{tl(kalan)}</b></div>
              ) : null}

              <p className="m-0 text-[13px] text-ink/55">Ödendi ve Teslim durumu tabloda her envanter için ayrı işaretlenir. Stok yalnızca teslimde düşer.</p>

              </>
            )}
            {hicEnvanterYok ? (
              <div>
                <label htmlFor="envanter-odenen" className="mb-1 block text-[13.5px] text-ink/70">Ödenen tutar</label>
                <Input id="envanter-odenen" type="number" placeholder="0" disabled className="text-[16px]" />
              </div>
            ) : null}
            {bilgi ? <p className={cn('m-0 text-[13.5px]', bilgi.renk)}>{bilgi.metin}</p> : null}
            {kayitHatasi ? (
              <div role="alert" className="flex items-start gap-2 border border-hata-600 bg-hata-100 px-3 py-2 text-[14px] text-hata-800">
                <TriangleAlert size={16} className="mt-0.5 shrink-0" />
                <span>{kayitHatasi}</span>
              </div>
            ) : null}
            <div className="flex gap-2">
              <Button variant="primary" className="flex-1" disabled={kaydetPasif} onClick={kaydet}>
                {kaydediyor ? 'Kaydediliyor…' : duzenlenen ? 'Satışı güncelle' : 'Satışı kaydet'}
              </Button>
              {duzenlenen ? (
                <Button variant="danger" onClick={sifirla}>
                  Vazgeç
                </Button>
              ) : null}
            </div>
          </div>
        </div>

        <h4 className="mt-1 text-[20px]">Ödenenler</h4>
        {(() => {
          const durumTag = (s) =>
            s.odemeDurumu === 'ODENDI' ? (
              <Tag tone="neutral" className="bg-basari-100 text-basari-800">Ödendi</Tag>
            ) : (
              <Tag tone="warning">Kısmi ödendi</Tag>
            );
          const ad = (k) => adBeden(TURLER.find((t) => t.kod === k.tur), k.beden);
          const chipler = (liste, tone, cls) =>
            liste.length === 0 ? (
              <span className="text-ink/55">—</span>
            ) : (
              <div className="flex flex-wrap gap-1">
                {liste.map((k) => (
                  <Tag key={k.tur} tone={tone} className={cls}>{ad(k)}</Tag>
                ))}
              </div>
            );
          const islemler = (s) => (
            <>
              <Button variant="guncelle" size="sm" onClick={() => duzenlemeyeBasla(s)}>
                Güncelle
              </Button>
              <Button variant="danger" size="sm" className="ml-1" onClick={() => setSilinecek(s)}>
                Sil
              </Button>
            </>
          );
          if (satislar.length === 0) {
            return mobil ? (
              <div className="py-4 text-center text-ink/55">Henüz envanter satışı yok.</div>
            ) : (
              <Table>
                <tbody>
                  <Bos colSpan={8}>Henüz envanter satışı yok.</Bos>
                </tbody>
              </Table>
            );
          }
          const topTutar = satislar.reduce((t, s) => t + Number(s.setTutari), 0);
          const topOdenen = satislar.reduce((t, s) => t + Number(s.odenenTutar), 0);
          const topKalan = satislar.reduce((t, s) => t + Number(s.kalanTutar), 0);
          return mobil ? (
            <div className="flex flex-col">
              {satislar.map((s) => (
                <div key={s.id} className={cn('border-b border-hairline py-3', duzenlenen?.id === s.id && 'bg-accent-100')}>
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-semibold">{s.setAd || 'Özel satış'}</span>
                    {durumTag(s)}
                    <span className="ml-auto text-[13px] text-ink/55">{tarihSaat(s.kayitTarihi)}</span>
                  </div>
                  <div className="mt-1.5 flex gap-5 text-[14.5px]">
                    <span>Ödenen: <b className="tabular-nums">{tl(s.odenenTutar)}</b><span className="text-[13px] text-ink/55"> · {yontemler(s)}</span></span>
                    <span>Kalan: <b className="tabular-nums">{Number(s.kalanTutar) > 0 ? tl(s.kalanTutar) : '—'}</b></span>
                  </div>
                  <div className="mt-2 text-[12.5px] uppercase tracking-[0.08em] text-ink/60">Teslim edilenler</div>
                  {chipler(s.kalemler.filter((k) => k.teslimEdildi), 'accent', 'bg-accent-200 text-accent-800')}
                  <div className="mt-2 text-[12.5px] uppercase tracking-[0.08em] text-ink/60">Eksik envanter</div>
                  {chipler(s.kalemler.filter((k) => !k.teslimEdildi), 'warning', '')}
                  <div className="mt-3">{islemler(s)}</div>
                </div>
              ))}
              <div className="flex flex-col gap-1 py-3 text-[14.5px]">
                <div className="flex justify-between"><span>Toplam ödenen</span><b className="tabular-nums">{tl(topOdenen)}</b></div>
                <div className="flex justify-between"><span>Toplam kalan</span><b className="tabular-nums">{topKalan > 0 ? tl(topKalan) : '—'}</b></div>
                <div className="mt-1 flex justify-between border-t-2 border-ink/60 pt-1.5"><span>Toplam tutar <span className="text-[13px] text-ink/60">(ödenen + kalan)</span></span><b className="tabular-nums">{tl(topTutar)}</b></div>
              </div>
            </div>
          ) : (
            <Table>
              <thead>
                <tr>
                  <Th>Envanter</Th>
                  <Th className="text-right">Ödenen tutar</Th>
                  <Th className="text-right">Kalan tutar</Th>
                  <Th>Ödeme şekli</Th>
                  <Th>Ödeme durumu</Th>
                  <Th>Ödeme tarihi</Th>
                  <Th>Teslim edilenler</Th>
                  <Th>Eksik envanter</Th>
                  <Th />
                </tr>
              </thead>
              <tbody>
                {satislar.map((s) => (
                  <Tr key={s.id} className={cn(duzenlenen?.id === s.id && 'bg-accent-100')}>
                    <Td>{s.setAd || 'Özel satış'}</Td>
                    <Td className="whitespace-nowrap text-right tabular-nums">{tl(s.odenenTutar)}</Td>
                    <Td className="whitespace-nowrap text-right font-bold tabular-nums">
                      {Number(s.kalanTutar) > 0 ? tl(s.kalanTutar) : <span className="font-normal text-ink/55">—</span>}
                    </Td>
                    <Td className="whitespace-nowrap">{yontemler(s, true)}</Td>
                    <Td>{durumTag(s)}</Td>
                    <Td className="whitespace-nowrap">{tarihSaat(s.kayitTarihi)}</Td>
                    <Td>{chipler(s.kalemler.filter((k) => k.teslimEdildi), 'accent', 'bg-accent-200 text-accent-800')}</Td>
                    <Td>{chipler(s.kalemler.filter((k) => !k.teslimEdildi), 'warning', '')}</Td>
                    <Td className="whitespace-nowrap text-right">{islemler(s)}</Td>
                  </Tr>
                ))}
                <tr>
                  <Td className="font-bold">Toplam</Td>
                  <Td className="whitespace-nowrap text-right font-bold tabular-nums">{tl(topOdenen)}</Td>
                  <Td className="whitespace-nowrap text-right font-bold tabular-nums">{topKalan > 0 ? tl(topKalan) : '—'}</Td>
                  <Td colSpan={6} />
                </tr>
                <tr>
                  <Td className="whitespace-nowrap border-t-2 border-ink/60 font-bold">Toplam tutar</Td>
                  <Td colSpan={2} className="whitespace-nowrap border-t-2 border-ink/60 text-right font-bold tabular-nums">{tl(topTutar)}</Td>
                  <Td colSpan={6} className="text-[13.5px] text-ink/60">= Ödenen tutar + Kalan tutar</Td>
                </tr>
              </tbody>
            </Table>
          );
        })()}
      </Card>

      <ConfirmDialog
        open={!!silinecekOdeme}
        title="Ödeme silinsin mi?"
        description="Bu ödeme satıştan düşülür; kalan tutar artar."
        isim={silinecekOdeme ? tl(silinecekOdeme.tutar) + ' · ' + tarihSaat(silinecekOdeme.odemeTarihi) : ''}
        confirmLabel="Sil"
        tehlikeli
        onConfirm={odemeSil}
        onCancel={() => setSilinecekOdeme(null)}
      />

      <ConfirmDialog
        open={!!silinecek}
        title="Envanter satışı silinsin mi?"
        description="Bu işlem geri alınamaz. Teslim edilmiş envanterlerin stoğu geri döner."
        isim={silinecek ? 'Envanter seti · ' + tl(silinecek.setTutari) : ''}
        confirmLabel="Sil"
        tehlikeli
        onConfirm={sil}
        onCancel={() => setSilinecek(null)}
      />
    </div>
  );
}
