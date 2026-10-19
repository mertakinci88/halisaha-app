import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CalendarDays, ChevronLeft, ChevronRight } from 'lucide-react';
import { PageHeader } from '@/components/AppLayout';
import { Button } from '@/components/ui/button';
import { Seg, SegOpt } from '@/components/ui/seg';
import { cn } from '@/lib/utils';
import { Yukleniyor, Hata } from '@/components/ui/durum';
import { rezervasyonService, sahaService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata } from '@/context/ToastContext';
import { gunSaati, hh, sureSaat, tl, toIsoDate, uzunTarih } from '@/lib/format';

const ACILIS = 12;
const KAPANIS = 26; // 26 = ertesi gün 02:00
// Takvim 30 dakikalık satırlardan oluşur: 12:00, 12:30, ... 02:00, 02:30 (rezervasyon tam saatte ya da buçukta başlar)
const YARIM_SAATLER = Array.from({ length: (KAPANIS - ACILIS + 1) * 2 }, (_, i) => ACILIS + i / 2);
const SATIR_YUKSEKLIGI = 32; // px; 1 saatlik rezervasyon 2 satır (64px) kaplar
const KENAR = 14; // sol kenar şeridi: yalnızca o saatte iptal edilmiş rezervasyon varsa ayrılır
const EN_KISA_SURE_SATIR = 2; // en kısa rezervasyon 1 saat = 2 satır
// Boşluğu 1 saatten kısa (30 dakika) olan satırlarda rezervasyon yapılamaz: taralı gösterilir ve "30 DK BOŞ" yazar
const TARAMA = 'repeating-linear-gradient(135deg, transparent 0, transparent 5px, rgba(29, 31, 32, 0.07) 5px, rgba(29, 31, 32, 0.07) 6px)';

export default function TakvimPage() {
  const navigate = useNavigate();
  const [gun, setGun] = useState(() => new Date());
  const [sahalar, setSahalar] = useState([]);
  const [rezervasyonlar, setRezervasyonlar] = useState([]);
  const [iptaller, setIptaller] = useState([]);
  const [hata, setHata] = useHata();
  const [yukleniyor, setYukleniyor] = useState(true);

  const yukle = useCallback(() => {
    setYukleniyor(true);
    setHata('');
    const ertesi = new Date(gun);
    ertesi.setDate(ertesi.getDate() + 1);
    const gunIso = toIsoDate(gun);
    // Takvim günü 12:00'dan ertesi gün 02:00'ye uzanır: iki günü çekip mutlak saate çeviriyoruz
    Promise.all([
      sahaService.list(),
      rezervasyonService.list({ tarih: gunIso }),
      rezervasyonService.list({ tarih: toIsoDate(ertesi) }),
    ])
      .then(([s, bugun, yarin]) => {
        const r = [...bugun, ...yarin]
          .map((x) => ({
            ...x,
            bas: (x.baslangicTarih.slice(0, 10) === gunIso ? 0 : 24) + gunSaati(x.baslangicTarih),
          }))
          .filter((x) => x.bas >= ACILIS && x.bas < KAPANIS + 1);
        setSahalar(s.filter((x) => x.durum === 'AKTIF'));
        setRezervasyonlar(r.filter((x) => x.durum === 'AKTIF'));
        setIptaller(r.filter((x) => x.durum === 'IPTAL'));
      })
      .catch((e) => setHata(apiHata(e), { sessiz: true }))
      .finally(() => setYukleniyor(false));
  }, [gun]);

  useEffect(() => {
    yukle();
  }, [yukle]);

  const gunKaydir = (fark) => {
    const d = new Date(gun);
    d.setDate(d.getDate() + fark);
    setGun(d);
  };

  const tarihSec = (e) => {
    if (e.target.value) setGun(new Date(e.target.value + 'T00:00:00'));
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

  // Her sahada rezervasyonlarla dolu 30 dakikalık satırlar (satır numarası: 12:00 = 0, 12:30 = 1 ...)
  const doluSatirlar = useMemo(() => {
    const harita = new Map();
    rezervasyonlar.forEach((r) => {
      const kume = harita.get(r.sahaId) || new Set();
      const bas = Math.round((r.bas - ACILIS) * 2);
      const bit = Math.round((r.bas + sureSaat(r.baslangicTarih, r.bitisTarih) - ACILIS) * 2);
      for (let i = bas; i < bit; i += 1) kume.add(i);
      harita.set(r.sahaId, kume);
    });
    return harita;
  }, [rezervasyonlar]);

  /** Bu saha ve saat aralığında (başlangıç/bitiş, saat olarak) iptal edilmiş bir rezervasyon var mı? Varsa sol kenar şeridi için yer ayrılır. */
  const iptalVar = (sahaId, bas, bit) =>
    iptaller.some((r) => r.sahaId === sahaId && r.bas < bit && r.bas + sureSaat(r.baslangicTarih, r.bitisTarih) > bas);

  /** Rezervasyonun sütun içindeki konumu: 30 dakikalık satır sayısı üzerinden (üstten uzaklık ve yükseklik, px) */
  const konum = (r) => ({
    top: (r.bas - ACILIS) * 2 * SATIR_YUKSEKLIGI,
    height: Math.max(sureSaat(r.baslangicTarih, r.bitisTarih), 0.5) * 2 * SATIR_YUKSEKLIGI,
  });

  return (
    <div>
      <PageHeader kicker="Rezervasyon" baslik="Rezervasyon takvimi" />

      <div className="mb-4 flex flex-wrap items-center gap-3.5">
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
              value={toIsoDate(gun)}
              onChange={tarihSec}
              onClick={tarihAc}
              title="Tarih seç"
              aria-label={'Tarih seç: ' + uzunTarih(gun)}
              className="absolute inset-0 h-full w-full cursor-pointer opacity-0 [font-size:16px]"
            />
          </div>
          <SegOpt onClick={() => gunKaydir(1)} aria-label="Sonraki gün">
            <ChevronRight size={16} strokeWidth={1.5} />
          </SegOpt>
        </Seg>
        <Button variant="gri" onClick={() => setGun(new Date())}>
          <CalendarDays size={16} strokeWidth={1.5} />
          <span>Bugün</span>
        </Button>

        <div className="ml-auto flex flex-wrap items-center gap-4 text-[13.5px]">
          <div className="flex items-center gap-2">
            <span className="h-3.5 w-3.5 border border-accent-400 bg-accent-200" />
            <span>Dolu</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="h-3.5 w-3.5 border border-basari-300 bg-basari-100" />
            <span>Abone</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="h-3.5 w-3.5 border border-divider" />
            <span>Boş</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="h-3.5 w-3.5 border border-dashed border-ink/30 bg-ink/5" />
            <span>İptal</span>
          </div>
          <div className="flex items-center gap-2" title="Boşluk 1 saatten kısa: bu satırdan başlayan rezervasyon yapılamaz">
            <span className="h-3.5 w-3.5 border border-divider" style={{ backgroundImage: TARAMA }} />
            <span>30 dk boş</span>
          </div>
        </div>
      </div>

      <Hata mesaj={hata} onTekrar={yukle} />

      {yukleniyor ? (
        <Yukleniyor metin="Takvim yükleniyor…" />
      ) : sahalar.length === 0 ? (
        <div className="py-10 text-center text-ink/55">
          Aktif saha bulunmuyor. Önce <a href="/sahalar/yeni">bir saha tanımlayın</a>.
        </div>
      ) : (
        <div className="blueprint">
          <i className="corner tl" />
          <i className="corner tr" />
          <i className="corner bl" />
          <i className="corner br" />

          <div className="flex border-b border-divider bg-surface">
            <div className="shrink-0 basis-[54px] sm:basis-[82px] px-2 py-2.5 text-[11.5px] uppercase tracking-[0.1em] text-ink/55">
              Saat
            </div>
            {sahalar.map((s) => (
              <div key={s.id} className="min-w-0 flex-1 border-l border-divider px-3 py-2.5">
                <div className="font-heading text-[16.5px] font-semibold">{s.ad}</div>
                <div className="text-[12.5px] text-ink/55">{tl(s.saatlikUcret)}/saat</div>
              </div>
            ))}
          </div>

          <div className="flex">
            <div className="shrink-0 basis-[54px] sm:basis-[82px]">
              {YARIM_SAATLER.map((saat) => {
                const tam = Number.isInteger(saat);
                return (
                  <div
                    key={saat}
                    style={{ height: SATIR_YUKSEKLIGI }}
                    className={cn(
                      'px-2 pt-1 tabular-nums',
                      tam ? 'border-t border-divider text-[13.5px] text-ink/60' : 'border-t border-dashed border-hairline text-[11.5px] text-ink/35',
                    )}
                  >
                    {hh(saat)}
                  </div>
                );
              })}
            </div>

            {sahalar.map((s) => (
              <div
                key={s.id}
                style={{ height: YARIM_SAATLER.length * SATIR_YUKSEKLIGI }}
                className="relative min-w-0 flex-1 border-l border-divider"
              >
                {/* Boş yarım saatler: tıklayınca o saatten başlayan yeni rezervasyon açılır */}
                {YARIM_SAATLER.map((saat, sira) => {
                  const dolu = doluSatirlar.get(s.id) || new Set();
                  // Satır boş ama en kısa rezervasyon (1 saat) sığmıyor: bir sonraki yarım saat dolu
                  const sigmaz = !dolu.has(sira) && Array.from({ length: EN_KISA_SURE_SATIR - 1 }, (_, k) => sira + 1 + k).some((k) => dolu.has(k));
                  return (
                    <button
                      key={saat}
                      type="button"
                      disabled={sigmaz}
                      style={{ height: SATIR_YUKSEKLIGI, paddingLeft: iptalVar(s.id, saat, saat + 0.5) ? KENAR : 0, backgroundImage: sigmaz ? TARAMA : undefined }}
                      title={sigmaz ? 'Bu saatte sadece 30 dakika boşluk var; en az 1 saatlik rezervasyon yapılabilir' : undefined}
                      onClick={() => navigate('/rezervasyon/yeni?sahaId=' + s.id + '&saat=' + saat + '&tarih=' + toIsoDate(gun))}
                      className={cn(
                        'grid w-full place-items-center border-0 border-t bg-transparent font-heading text-[12px] font-semibold tracking-[0.04em]',
                        Number.isInteger(saat) ? 'border-divider' : 'border-dashed border-hairline',
                        sigmaz
                          ? 'cursor-not-allowed text-ink/30'
                          : 'cursor-pointer text-accent-700/40 hover:bg-ink/4 hover:text-accent-700',
                      )}
                      aria-label={s.ad + ' ' + hh(saat) + (sigmaz ? ' sadece 30 dakika boş, rezervasyon yapılamaz' : ' rezervasyon ekle')}
                    >
                      {sigmaz ? '30 DK BOŞ' : '+ EKLE'}
                    </button>
                  );
                })}

                {/* İptal edilen rezervasyonlar: sol kenarda kesik çizgili şerit (üzerine gelince ad görünür) */}
                {iptaller
                  .filter((r) => r.sahaId === s.id)
                  .map((r) => {
                    const { top, height } = konum(r);
                    return (
                      <button
                        key={'iptal-' + r.id}
                        type="button"
                        onClick={() => navigate('/rezervasyon/' + r.id)}
                        title={r.adSoyad + ' · İptal edildi'}
                        style={{ top: top + 2, height: height - 4, width: KENAR - 4, left: 2 }}
                        className="absolute z-[1] cursor-pointer border border-dashed border-ink/30 bg-ink/10 p-0 hover:bg-ink/20"
                      />
                    );
                  })}

                {/* Aktif rezervasyonlar: başlangıç ve süresine göre yükseklikte bloklar */}
                {rezervasyonlar
                  .filter((r) => r.sahaId === s.id)
                  .map((rez) => {
                    const { top, height } = konum(rez);
                    const kisa = height < 56;
                    return (
                      <button
                        key={rez.id}
                        type="button"
                        onClick={() => navigate('/rezervasyon/' + rez.id)}
                        style={{
                          top: top + 2,
                          height: height - 4,
                          // İptal edilmiş bir rezervasyon varsa sol kenarda şerit için yer bırakılır; yoksa blok sütunun tamamını kaplar
                          left: iptalVar(s.id, rez.bas, rez.bas + sureSaat(rez.baslangicTarih, rez.bitisTarih)) ? KENAR : 2,
                          right: 2,
                        }}
                        className={cn(
                          'absolute z-[2] flex cursor-pointer flex-col justify-center gap-0.5 overflow-hidden border px-1.5 py-1 text-left font-body sm:px-2.5',
                          rez.antrenman
                            ? 'border-ink/25 bg-ink/10 text-ink'
                            : rez.abonelikId
                            ? 'border-basari-300 bg-basari-100 text-basari-800'
                            : 'border-accent-400 bg-accent-200 text-accent-900',
                        )}
                      >
                        <span className="max-w-full truncate text-[14.5px] font-medium">
                          {rez.adSoyad}
                          {kisa ? <span className="ml-2 hidden text-[12px] font-normal opacity-70 sm:inline">{hh(rez.bas)}</span> : null}
                        </span>
                        {kisa ? null : (
                          <span className="block truncate text-[12.5px] tabular-nums opacity-75">
                            <span className="hidden sm:inline">{hh(rez.bas)} – {hh(rez.bas + sureSaat(rez.baslangicTarih, rez.bitisTarih))} · </span>
                            {rez.antrenman ? (
                              <span className="font-semibold uppercase tracking-[0.1em]">Antrenman</span>
                            ) : rez.abonelikId && Number(rez.odenecekTutar) === 0 ? (
                              <span className="font-semibold uppercase tracking-[0.1em]">Abone</span>
                            ) : (
                              <>
                                {rez.abonelikId ? 'Abone · ' : ''}
                                {tl(rez.odenecekTutar)} ·{' '}
                                {rez.odemeDurumu === 'ODENDI'
                                  ? 'Ödendi'
                                  : rez.odemeDurumu === 'KISMI_ODENDI'
                                    ? 'Ödenen ' + tl(rez.odenenTutar)
                                    : 'Ödenmedi'}
                              </>
                            )}
                          </span>
                        )}
                      </button>
                    );
                  })}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
