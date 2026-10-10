import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { PageHeader } from '@/components/AppLayout';
import { Button } from '@/components/ui/button';
import { Seg, SegOpt } from '@/components/ui/seg';
import { Yukleniyor, Hata } from '@/components/ui/durum';
import { rezervasyonService, sahaService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata } from '@/context/ToastContext';
import { hh, saatSayisi, sureSaat, tl, toIsoDate, uzunTarih } from '@/lib/format';

const ACILIS = 12;
const KAPANIS = 26; // 26 = ertesi gün 02:00
const SAATLER = Array.from({ length: KAPANIS - ACILIS + 1 }, (_, i) => ACILIS + i);

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
            bas: (x.baslangicTarih.slice(0, 10) === gunIso ? 0 : 24) + saatSayisi(x.baslangicTarih),
          }))
          .filter((x) => x.bas >= ACILIS && x.bas <= KAPANIS);
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

  const kapsar = (r, sahaId, saat) => {
    if (r.sahaId !== sahaId) return false;
    return saat >= r.bas && saat < r.bas + sureSaat(r.baslangicTarih, r.bitisTarih);
  };

  const hucre = (sahaId, saat) => rezervasyonlar.find((r) => kapsar(r, sahaId, saat));
  const hucreIptaller = (sahaId, saat) => iptaller.filter((r) => kapsar(r, sahaId, saat));

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
        <Button variant="secondary" onClick={() => setGun(new Date())}>
          Bugün
        </Button>

        <div className="ml-auto flex flex-wrap items-center gap-4 text-[13.5px]">
          <div className="flex items-center gap-2">
            <span className="h-3.5 w-3.5 border border-accent-400 bg-accent-200" />
            <span>Dolu</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="h-3.5 w-3.5 border border-divider" />
            <span>Boş</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="h-3.5 w-3.5 border border-dashed border-ink/30 bg-ink/5" />
            <span>İptal</span>
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
            <div className="shrink-0 basis-[82px] px-2 py-2.5 text-[11.5px] uppercase tracking-[0.1em] text-ink/55">
              Saat
            </div>
            {sahalar.map((s) => (
              <div key={s.id} className="min-w-0 flex-1 border-l border-divider px-3 py-2.5">
                <div className="font-heading text-[16.5px] font-semibold">{s.ad}</div>
                <div className="text-[12.5px] text-ink/55">{tl(s.saatlikUcret)}/saat</div>
              </div>
            ))}
          </div>

          {SAATLER.map((saat) => (
            <div key={saat} className="flex border-b border-hairline">
              <div className="shrink-0 basis-[82px] p-2 text-[13.5px] tabular-nums text-ink/60">{hh(saat)}</div>
              {sahalar.map((s) => {
                const rez = hucre(s.id, saat);
                const iptal = rez ? [] : hucreIptaller(s.id, saat);
                return (
                  <div key={s.id} className="min-w-0 flex-1 border-l border-divider p-1">
                    {rez ? (
                      <button
                        type="button"
                        onClick={() => navigate('/rezervasyon/' + rez.id)}
                        className="flex h-[52px] w-full cursor-pointer flex-col justify-center gap-0.5 border border-accent-400 bg-accent-200 px-2.5 py-1 text-left font-body text-accent-900"
                      >
                        <span className="max-w-full truncate text-[14.5px] font-medium">{rez.adSoyad}</span>
                        <span className="text-[12.5px] tabular-nums opacity-75">
                          {tl(rez.odenecekTutar)} ·{' '}
                          {rez.odemeDurumu === 'ODENDI'
                            ? 'Ödendi'
                            : rez.odemeDurumu === 'KISMI_ODENDI'
                              ? 'Ödenen ' + tl(rez.odenenTutar)
                              : 'Ödenmedi'}
                        </span>
                      </button>
                    ) : (
                      <div className="flex h-[52px] flex-col gap-0.5">
                        {iptal.length > 0 && (
                          <button
                            type="button"
                            onClick={() => navigate('/rezervasyon/' + iptal[0].id)}
                            title={iptal.map((x) => x.adSoyad).join(', ') + ' · İptal edildi'}
                            className="flex h-[22px] w-full shrink-0 cursor-pointer items-center gap-1.5 border border-dashed border-ink/25 bg-ink/5 px-2 text-left font-body text-[12px] text-ink/50 hover:bg-ink/10"
                          >
                            <span className="truncate line-through">{iptal[0].adSoyad}</span>
                            {iptal.length > 1 && <span className="shrink-0">+{iptal.length - 1}</span>}
                            <span className="ml-auto shrink-0 text-[10.5px] uppercase tracking-[0.08em]">İptal</span>
                          </button>
                        )}
                        <button
                          type="button"
                          onClick={() =>
                            navigate('/rezervasyon/yeni?sahaId=' + s.id + '&saat=' + saat + '&tarih=' + toIsoDate(gun))
                          }
                          className="grid min-h-0 w-full flex-1 cursor-pointer place-items-center border border-dashed border-ink/15 bg-transparent font-heading text-[13.5px] font-semibold tracking-[0.04em] text-accent-700 opacity-40 hover:opacity-100 hover:bg-ink/4"
                        >
                          + EKLE
                        </button>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
