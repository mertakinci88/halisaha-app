import { useCallback, useEffect, useMemo, useState } from 'react';
import { CalendarDays, Check, ChevronLeft, ChevronRight, ClipboardCheck, Save, Search } from 'lucide-react';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input, Select } from '@/components/ui/input';
import { Seg, SegOpt } from '@/components/ui/seg';
import { Tag } from '@/components/ui/tag';
import { ConfirmDialog } from '@/components/ui/dialog';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import { grupService, yoklamaService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata, useToast } from '@/context/ToastContext';
import { kisaTarih, toIsoDate, uzunTarih } from '@/lib/format';
import { cn } from '@/lib/utils';

const DURUMLAR = [
  { kod: 'GELDI', etiket: 'Geldi' },
  { kod: 'GELMEDI', etiket: 'Gelmedi' },
  { kod: 'IZINLI', etiket: 'İzinli' },
];

const KUTU = {
  GELDI: 'border-basari-600 bg-basari-600',
  GELMEDI: 'border-hata-600 bg-hata-600',
  IZINLI: 'border-accent bg-accent',
};
const YAZI = { GELDI: 'text-basari-800', GELMEDI: 'text-hata-800', IZINLI: 'text-accent-800' };
const SATIR = { GELDI: 'bg-basari-100/60', GELMEDI: 'bg-hata-100/70', IZINLI: 'bg-accent-100' };

const bugunMu = (d) => toIsoDate(d) === toIsoDate(new Date());

/** Üç seçenekten yalnızca biri işaretlenebilir; biri seçilince diğerleri pasifleşir, işaret kalkınca yeniden açılır. */
function DurumKutusu({ kod, etiket, durum, onSec, ad }) {
  const secili = durum === kod;
  const pasif = durum !== null && !secili;
  return (
    <label
      className={cn(
        'inline-flex min-h-10 cursor-pointer items-center gap-2 pr-3 text-[15px]',
        pasif && 'cursor-not-allowed opacity-35',
      )}
    >
      <input
        type="checkbox"
        className="peer sr-only"
        checked={secili}
        disabled={pasif}
        onChange={() => onSec(secili ? null : kod)}
        aria-label={ad + ': ' + etiket}
      />
      <span
        className={cn(
          'grid size-[21px] shrink-0 place-items-center border-[1.5px] border-ink/50 bg-bg text-white',
          'peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-accent',
          secili && KUTU[kod],
          pasif && 'border-ink/30 bg-surface',
        )}
      >
        {secili ? <Check size={15} strokeWidth={3} /> : null}
      </span>
      <span className={cn(secili && ['font-bold', YAZI[kod]])}>{etiket}</span>
    </label>
  );
}

export default function YoklamaPage() {
  const toast = useToast();
  const [gun, setGun] = useState(() => new Date());
  const [gruplar, setGruplar] = useState([]);
  const [grupId, setGrupId] = useState('');
  const [arama, setArama] = useState('');
  const [satirlar, setSatirlar] = useState([]);
  const [kayitli, setKayitli] = useState([]); // sunucudaki hâl; değişiklik var mı diye karşılaştırılır
  const [hata, setHata] = useHata();
  const [yukleniyor, setYukleniyor] = useState(false);
  const [kaydediyor, setKaydediyor] = useState(false);
  const [bekleyen, setBekleyen] = useState(null); // kaydedilmemiş değişiklik varken tarih/grup değişimi onayı

  const gunIso = toIsoDate(gun);
  const degisti = useMemo(() => JSON.stringify(satirlar) !== JSON.stringify(kayitli), [satirlar, kayitli]);

  useEffect(() => {
    grupService
      .list()
      .then((liste) => setGruplar([...liste].sort((a, b) => a.ad.localeCompare(b.ad, 'tr'))))
      .catch((e) => setHata(apiHata(e), { sessiz: true }));
  }, []);

  const yukle = useCallback(() => {
    if (!grupId) {
      setSatirlar([]);
      setKayitli([]);
      return;
    }
    setYukleniyor(true);
    setHata('');
    yoklamaService
      .list(gunIso, grupId)
      .then((liste) => {
        const hazir = liste.map((s) => ({ ogrenciId: s.ogrenciId, adSoyad: s.adSoyad, durum: s.durum, not: s.not || '' }));
        setSatirlar(hazir);
        setKayitli(hazir);
      })
      .catch((e) => setHata(apiHata(e), { sessiz: true }))
      .finally(() => setYukleniyor(false));
  }, [gunIso, grupId]);

  useEffect(() => {
    yukle();
  }, [yukle]);

  /** Kaydedilmemiş değişiklik varsa önce onay ister. */
  const onayliDegistir = (islem) => (degisti ? setBekleyen(() => islem) : islem());

  const gunKaydir = (fark) => {
    const d = new Date(gun);
    d.setDate(d.getDate() + fark);
    if (toIsoDate(d) > toIsoDate(new Date())) return; // gelecek günün yoklaması alınamaz
    onayliDegistir(() => setGun(d));
  };

  const tarihSec = (e) => {
    if (!e.target.value || e.target.value > toIsoDate(new Date())) return;
    const d = new Date(e.target.value + 'T00:00:00');
    onayliDegistir(() => setGun(d));
  };

  // Masaüstü tarayıcılar tıklamada tarih seçiciyi kendiliğinden açmaz; showPicker ile açılır.
  // Mobilde (iOS Safari dahil) seçici, kullanıcının doğrudan input'a dokunmasıyla açılır.
  const tarihAc = (e) => {
    try {
      e.currentTarget.showPicker?.();
    } catch {
      // showPicker desteklenmiyorsa/engellenmişse yerleşik davranış yeterli
    }
  };

  const durumSec = (ogrenciId, durum) =>
    setSatirlar((p) => p.map((s) => (s.ogrenciId === ogrenciId ? { ...s, durum, not: durum ? s.not : '' } : s)));

  const notYaz = (ogrenciId, not) =>
    setSatirlar((p) => p.map((s) => (s.ogrenciId === ogrenciId ? { ...s, not } : s)));

  const kaydet = async () => {
    setKaydediyor(true);
    setHata('');
    try {
      const liste = await yoklamaService.kaydet(
        gunIso,
        grupId,
        satirlar.map((s) => ({ ogrenciId: s.ogrenciId, durum: s.durum, not: s.not.trim() || null })),
      );
      const hazir = liste.map((s) => ({ ogrenciId: s.ogrenciId, adSoyad: s.adSoyad, durum: s.durum, not: s.not || '' }));
      setSatirlar(hazir);
      setKayitli(hazir);
      const grupAd = gruplar.find((g) => String(g.id) === String(grupId))?.ad;
      toast.basari('Yoklama kaydedildi: ' + grupAd + ', ' + kisaTarih(gunIso) + '.');
    } catch (e) {
      setHata(apiHata(e));
    } finally {
      setKaydediyor(false);
    }
  };

  const sayi = (kod) => satirlar.filter((s) => s.durum === kod).length;
  const isaretsiz = satirlar.filter((s) => !s.durum).length;
  const aranan = arama.trim().toLocaleLowerCase('tr');
  const gorunen = aranan ? satirlar.filter((s) => s.adSoyad.toLocaleLowerCase('tr').includes(aranan)) : satirlar;
  const grupAd = gruplar.find((g) => String(g.id) === String(grupId))?.ad;

  return (
    <div>
      <PageHeader kicker="Antrenman" baslik="Yoklama" />
      <Hata mesaj={hata} onTekrar={grupId ? yukle : undefined} />

      <Card className="mb-5">
        <div className="flex flex-wrap items-center gap-3.5">
          <Seg>
            <SegOpt onClick={() => gunKaydir(-1)} aria-label="Önceki gün">
              <ChevronLeft size={16} strokeWidth={1.5} />
            </SegOpt>
            <div className="relative flex min-w-[210px] border-l border-r border-divider hover:bg-ink/5">
              <span
                aria-hidden="true"
                className="pointer-events-none flex w-full items-center justify-center px-3.5 font-heading text-[15.5px] font-semibold"
              >
                {uzunTarih(gun)}
              </span>
              {/* Dokunmayı doğrudan input alır (iOS'ta tarih seçici ancak böyle açılır); yazı üstüne görünmez şekilde biner */}
              <input
                type="date"
                value={gunIso}
                max={toIsoDate(new Date())}
                onChange={tarihSec}
                onClick={tarihAc}
                title="Tarih seç"
                aria-label={'Tarih seç: ' + uzunTarih(gun)}
                className="absolute inset-0 h-full w-full cursor-pointer opacity-0 [font-size:16px]"
              />
            </div>
            <SegOpt onClick={() => gunKaydir(1)} aria-label="Sonraki gün" disabled={bugunMu(gun)} className="disabled:cursor-not-allowed disabled:opacity-35">
              <ChevronRight size={16} strokeWidth={1.5} />
            </SegOpt>
          </Seg>
          <Button variant="gri" onClick={() => onayliDegistir(() => setGun(new Date()))} disabled={bugunMu(gun)}>
            <CalendarDays size={16} strokeWidth={1.5} />
            <span>Bugün</span>
          </Button>
        </div>
      </Card>

      <Card className="gap-3.5">
        <div className="flex flex-wrap items-end gap-3">
          <div className="min-w-0 flex-1 sm:max-w-[520px]">
            <label htmlFor="yoklama-ara" className="mb-1 block text-[13.5px] text-ink/70">
              Öğrenci ara
            </label>
            <div className="relative">
              <Search size={16} strokeWidth={1.5} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ink/55" />
              <Input
                id="yoklama-ara"
                type="search"
                placeholder="Öğrenci ara (ad soyad)…"
                value={arama}
                onChange={(e) => setArama(e.target.value)}
                disabled={!grupId}
                className="pl-9 text-[16px]"
              />
            </div>
          </div>
          <div className="w-full sm:w-[300px]">
            <label htmlFor="yoklama-grup" className="mb-1 block text-[13.5px] text-ink/70">
              Grup
            </label>
            <Select
              id="yoklama-grup"
              value={grupId}
              onChange={(e) => {
                const yeni = e.target.value;
                onayliDegistir(() => {
                  setGrupId(yeni);
                  setArama('');
                });
              }}
              className="text-[16px]"
            >
              <option value="">Grup seçin…</option>
              {gruplar.map((g) => (
                <option key={g.id} value={g.id}>
                  {g.ad}
                </option>
              ))}
            </Select>
          </div>
        </div>

        {!grupId ? (
          <div className="flex flex-col items-center justify-center gap-2.5 border border-dashed border-ink/30 px-5 py-14 text-center text-ink/55">
            <ClipboardCheck size={40} strokeWidth={1.25} />
            <div className="font-heading text-[22px] font-semibold text-ink/75">Yoklama almak için bir grup seçin</div>
            <div>Seçtiğiniz grubun öğrencileri burada listelenecek.</div>
          </div>
        ) : yukleniyor ? (
          <Yukleniyor />
        ) : satirlar.length === 0 ? (
          <div className="border border-dashed border-ink/30 px-5 py-10 text-center text-ink/55">
            Bu grupta aktif öğrenci yok.
          </div>
        ) : (
          <>
            <div className="flex flex-wrap items-center gap-2">
              <Tag tone="neutral" className="bg-basari-100 text-basari-800">Geldi {sayi('GELDI')}</Tag>
              <Tag tone="danger" className="bg-hata-100 text-hata-800">Gelmedi {sayi('GELMEDI')}</Tag>
              <Tag tone="accent" className="bg-accent-200 text-accent-800">İzinli {sayi('IZINLI')}</Tag>
              <Tag tone="neutral">İşaretlenmemiş {isaretsiz}</Tag>
            </div>

            <div>
              <div className="hidden border-b border-divider px-2 py-2 text-[12.5px] uppercase tracking-[0.08em] text-ink/60 md:grid md:grid-cols-[minmax(0,300px)_380px_minmax(0,1fr)]">
                <span>Ad soyad</span>
                <span>Durum</span>
                <span>Not</span>
              </div>
              {gorunen.length === 0 ? (
                <div className="px-2 py-8 text-center text-ink/55">Aramaya uygun öğrenci yok.</div>
              ) : (
                gorunen.map((s) => (
                  <div
                    key={s.ogrenciId}
                    className={cn(
                      'grid gap-x-2 gap-y-1 border-b border-hairline px-2 py-2.5 md:grid-cols-[minmax(0,300px)_380px_minmax(0,1fr)] md:items-center',
                      s.durum && SATIR[s.durum],
                    )}
                  >
                    <div className="text-[15.5px]">{s.adSoyad}</div>
                    <div className="flex flex-wrap items-center">
                      {DURUMLAR.map((d) => (
                        <DurumKutusu
                          key={d.kod}
                          kod={d.kod}
                          etiket={d.etiket}
                          durum={s.durum}
                          ad={s.adSoyad}
                          onSec={(yeni) => durumSec(s.ogrenciId, yeni)}
                        />
                      ))}
                    </div>
                    <Input
                      type="text"
                      value={s.not}
                      maxLength={255}
                      disabled={!s.durum}
                      placeholder={s.durum ? 'Not ekle…' : 'Önce durum seçin'}
                      aria-label={s.adSoyad + ' notu'}
                      onChange={(e) => notYaz(s.ogrenciId, e.target.value)}
                      className="text-[16px] disabled:opacity-60"
                    />
                  </div>
                ))
              )}
            </div>

            <div className="flex flex-wrap items-center justify-between gap-3">
              <span className="text-[13.5px] text-ink/55">
                {grupAd} · {satirlar.length} öğrenci · {kisaTarih(gunIso)}
                {degisti ? ' · kaydedilmemiş değişiklik var' : ''}
              </span>
              <Button variant="primary" onClick={kaydet} disabled={kaydediyor || !degisti} className="max-sm:w-full">
                <Save size={16} strokeWidth={1.5} />
                <span>{kaydediyor ? 'Kaydediliyor…' : 'Yoklamayı kaydet'}</span>
              </Button>
            </div>
          </>
        )}
      </Card>

      <ConfirmDialog
        open={!!bekleyen}
        title="Kaydedilmemiş değişiklikler var"
        description="Devam ederseniz bu yoklamadaki işaretlemeler kaybolur. Devam edilsin mi?"
        confirmLabel="Devam et"
        onConfirm={() => {
          const islem = bekleyen;
          setBekleyen(null);
          islem();
        }}
        onCancel={() => setBekleyen(null)}
      />
    </div>
  );
}
