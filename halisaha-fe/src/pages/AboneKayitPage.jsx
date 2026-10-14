import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { TriangleAlert } from 'lucide-react';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Field, Input, Select } from '@/components/ui/input';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import HesapSecimi from '@/components/HesapSecimi';
import { aboneService, sahaService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata, useToast } from '@/context/ToastContext';
import { hh, kisaTarih, ODEME_YONTEMI_LABEL, telMaske, telRakam, tl, toIsoDate, tutarSinirla } from '@/lib/format';
import { ACILIS, EN_UZUN_SURE, HAFTA_GUNLERI, KAPANIS, birYilSonra, gunAdi, haftaGunuOf } from '@/lib/abone';

const BUGUN = toIsoDate(new Date());

export default function AboneKayitPage() {
  const navigate = useNavigate();
  const toast = useToast();
  const [hata, setHata] = useHata();
  const [sahalar, setSahalar] = useState(null);
  const [kaydediyor, setKaydediyor] = useState(false);
  const [onizleme, setOnizleme] = useState(null);
  const [odenenElle, setOdenenElle] = useState(false);
  const [gunElle, setGunElle] = useState(false);
  const [standart, setStandart] = useState(null);
  const [form, setForm] = useState({
    adSoyad: '',
    telNo: '',
    sahaId: '',
    haftaGunu: haftaGunuOf(BUGUN),
    baslangicSaat: 19,
    sureSaat: 1,
    baslangic: BUGUN,
    ucret: '',
    odenen: '',
    odemeSekli: '',
    hesapId: '',
    odemeTarihi: BUGUN,
    not: '',
  });
  const set = (k, v) => setForm((p) => ({ ...p, [k]: v }));

  useEffect(() => {
    Promise.all([sahaService.list(), aboneService.ayar()])
      .then(([s, ayar]) => {
        const aktif = s.filter((x) => x.durum === 'AKTIF');
        setSahalar(aktif);
        setStandart(ayar.standartUcret);
        setForm((p) => ({
          ...p,
          sahaId: aktif.length === 1 ? String(aktif[0].id) : p.sahaId,
          // Ücret standart tutardan dolar; elle değiştirilebilir
          ucret: ayar.standartUcret != null ? String(ayar.standartUcret) : p.ucret,
          odenen: ayar.standartUcret != null ? String(ayar.standartUcret) : p.odenen,
        }));
      })
      .catch((e) => {
        setSahalar([]);
        setHata(apiHata(e), { sessiz: true });
      });
  }, []);

  // Gün, kullanıcı seçmediyse başlangıç tarihinin gününe uyar
  useEffect(() => {
    if (!gunElle && form.baslangic) set('haftaGunu', haftaGunuOf(form.baslangic));
  }, [form.baslangic, gunElle]);

  // Alınan ödeme, elle değiştirilmediyse ücretle birlikte değişir (varsayılan: tamamı peşin)
  useEffect(() => {
    if (!odenenElle) set('odenen', form.ucret);
  }, [form.ucret, odenenElle]);

  // Seçime göre oluşacak rezervasyon sayısı ve çakışmalar
  useEffect(() => {
    if (!form.sahaId || !form.baslangic) {
      setOnizleme(null);
      return undefined;
    }
    let iptal = false;
    const t = setTimeout(() => {
      aboneService
        .onizle({
          sahaId: Number(form.sahaId),
          haftaGunu: Number(form.haftaGunu),
          baslangicSaat: Number(form.baslangicSaat),
          sureSaat: Number(form.sureSaat),
          baslangicTarih: form.baslangic,
        })
        .then((o) => !iptal && setOnizleme(o))
        .catch(() => !iptal && setOnizleme(null));
    }, 400);
    return () => {
      iptal = true;
      clearTimeout(t);
    };
  }, [form.sahaId, form.haftaGunu, form.baslangicSaat, form.sureSaat, form.baslangic]);

  const enUzun = Math.min(EN_UZUN_SURE, KAPANIS - Number(form.baslangicSaat));
  const bitis = onizleme?.bitisTarih || birYilSonra(form.baslangic);
  const ucret = Number(String(form.ucret).replace(',', '.')) || 0;
  const odenen = Number(String(form.odenen).replace(',', '.')) || 0;
  const cakisma = onizleme?.cakismalar?.length > 0;

  const kaydet = async (e) => {
    e.preventDefault();
    setHata('');
    if (!/^5\d{9}$/.test(form.telNo)) {
      setHata('Telefon numarası 10 haneli olmalı ve 5 ile başlamalıdır (örn. 546 976 46 13).');
      return;
    }
    if (!(ucret > 0)) {
      setHata('Abonelik ücretini giriniz.');
      return;
    }
    if (!(odenen > 0)) {
      setHata('Alınan ödeme tutarını giriniz.');
      return;
    }
    if (odenen > ucret) {
      setHata('Alınan ödeme abonelik ücretini (' + tl(ucret) + ') aşamaz.');
      return;
    }
    if (!form.odemeSekli) {
      setHata('Lütfen ödeme şeklini seçiniz.');
      return;
    }
    setKaydediyor(true);
    try {
      const a = await aboneService.create({
        sahaId: Number(form.sahaId),
        adSoyad: form.adSoyad.trim(),
        telNo: '0' + form.telNo,
        haftaGunu: Number(form.haftaGunu),
        baslangicSaat: Number(form.baslangicSaat),
        sureSaat: Number(form.sureSaat),
        baslangicTarih: form.baslangic,
        ucret,
        odenenTutar: odenen,
        odemeSekli: form.odemeSekli,
        hesapId: form.odemeSekli === 'HAVALE_EFT' && form.hesapId ? Number(form.hesapId) : null,
        odemeTarihi: form.odemeTarihi || null,
        not: form.not.trim() || null,
      });
      toast.basari('Abonelik oluşturuldu; ' + a.haftalar.length + ' haftalık rezervasyon takvime eklendi.');
      navigate('/aboneler/' + a.id, { replace: true });
    } catch (err) {
      setHata(apiHata(err));
    } finally {
      setKaydediyor(false);
    }
  };

  if (sahalar === null) return <Yukleniyor />;

  return (
    <div>
      <PageHeader kicker="Abonelik" baslik="Yeni abonelik" />
      <Hata mesaj={hata} />

      <form onSubmit={kaydet} className="grid items-start gap-5 [grid-template-columns:repeat(auto-fit,minmax(min(340px,100%),1fr))]">
        <Card className="gap-3.5">
          <h4 className="text-[22px]">Abone ve rezervasyon</h4>
          <Field label="Ad soyad">
            <Input value={form.adSoyad} onChange={(e) => set('adSoyad', e.target.value)} required maxLength={255} />
          </Field>
          <Field label="Telefon">
            <Input
              type="tel"
              inputMode="numeric"
              placeholder="0 (5xx) xxx xx xx"
              value={telMaske(form.telNo)}
              onChange={(e) => set('telNo', telRakam(e.target.value))}
              required
            />
          </Field>
          <Field label="Saha">
            <Select value={form.sahaId} onChange={(e) => set('sahaId', e.target.value)} required>
              <option value="">Seçiniz</option>
              {sahalar.map((s) => (
                <option key={s.id} value={s.id}>{s.ad}</option>
              ))}
            </Select>
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Başlangıç tarihi">
              <Input type="date" min={BUGUN} value={form.baslangic} onChange={(e) => set('baslangic', e.target.value)} required />
            </Field>
            <Field label="Bitiş tarihi">
              <Input type="text" value={kisaTarih(bitis)} readOnly disabled aria-readonly="true" title="Başlangıçtan 1 yıl sonrası; otomatik hesaplanır" />
            </Field>
          </div>
          <div className="grid grid-cols-3 gap-3">
            <Field label="Gün">
              <Select
                value={form.haftaGunu}
                onChange={(e) => {
                  setGunElle(true);
                  set('haftaGunu', Number(e.target.value));
                }}
              >
                {HAFTA_GUNLERI.map((g) => (
                  <option key={g.no} value={g.no}>{g.ad}</option>
                ))}
              </Select>
            </Field>
            <Field label="Saat">
              <Select
                value={form.baslangicSaat}
                onChange={(e) => {
                  const saat = Number(e.target.value);
                  set('baslangicSaat', saat);
                  if (form.sureSaat > KAPANIS - saat) set('sureSaat', KAPANIS - saat);
                }}
              >
                {Array.from({ length: KAPANIS - ACILIS }, (_, i) => ACILIS + i).map((s) => (
                  <option key={s} value={s}>{hh(s)}</option>
                ))}
              </Select>
            </Field>
            <Field label="Süre">
              <Select value={form.sureSaat} onChange={(e) => set('sureSaat', Number(e.target.value))}>
                {Array.from({ length: enUzun }, (_, i) => i + 1).map((s) => (
                  <option key={s} value={s}>{s} saat</option>
                ))}
              </Select>
            </Field>
          </div>

          {onizleme ? (
            cakisma ? (
              <div role="alert" className="flex items-start gap-2 border border-yellow-600 bg-yellow-100 px-3 py-2 text-[14px] text-yellow-800">
                <TriangleAlert size={16} className="mt-0.5 shrink-0" />
                <div>
                  Bu gün ve saatte {onizleme.cakismalar.length} tarihte saha dolu:{' '}
                  {onizleme.cakismalar.slice(0, 5).map((c) => kisaTarih(c.tarih) + ' (' + c.adSoyad + ')').join(', ')}
                  {onizleme.cakismalar.length > 5 ? ' ve diğerleri' : ''}. Önce bu rezervasyonları iptal edin ya da farklı bir gün/saat seçin.
                </div>
              </div>
            ) : (
              <p className="m-0 text-[14px] text-ink/70">
                Her <b>{gunAdi(form.haftaGunu)}</b> için toplam <b>{onizleme.rezervasyonSayisi}</b> haftalık rezervasyon oluşturulacak
                ({kisaTarih(onizleme.ilkTarih)} – {kisaTarih(onizleme.sonTarih)}).
              </p>
            )
          ) : null}
        </Card>

        <Card className="gap-3.5">
          <h4 className="text-[22px]">Ücret ve ödeme</h4>
          <Field label="Abonelik ücreti (₺)">
            <Input
              type="number"
              min="0.01"
              step="0.01"
              inputMode="decimal"
              value={form.ucret}
              onChange={(e) => {
                set('ucret', tutarSinirla(e.target.value, 10000000));
              }}
              required
            />
            <p className="m-0 mt-1 text-[13px] text-ink/55">
              {standart != null
                ? 'Standart ücret ' + tl(standart) + '; gerekirse bu abone için değiştirebilirsiniz.'
                : 'Standart ücret tanımlı değil; Aboneler sayfasından yönetici tanımlayabilir.'}
            </p>
          </Field>
          <Field label="Alınan ödeme (₺)">
            <Input
              type="number"
              min="0.01"
              step="0.01"
              inputMode="decimal"
              value={form.odenen}
              onChange={(e) => {
                setOdenenElle(true);
                set('odenen', tutarSinirla(e.target.value, ucret || 10000000));
              }}
              required
            />
            <p className="m-0 mt-1 text-[13px] text-ink/55">
              Ücretin tamamı ya da bir kısmı alınabilir; kalan tutar sonradan abone detayından tahsil edilir.
              {ucret > 0 && odenen > 0 && odenen < ucret ? ' Kalan: ' + tl(ucret - odenen) + '.' : ''}
            </p>
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Ödeme şekli">
              <Select
                value={form.odemeSekli}
                onChange={(e) => {
                  set('odemeSekli', e.target.value);
                  if (e.target.value !== 'HAVALE_EFT') set('hesapId', '');
                }}
                required
              >
                <option value="">Seçiniz</option>
                {Object.entries(ODEME_YONTEMI_LABEL).map(([kod, ad]) => (
                  <option key={kod} value={kod}>{ad}</option>
                ))}
              </Select>
            </Field>
            <Field label="Ödeme tarihi">
              <Input type="date" max={BUGUN} value={form.odemeTarihi} onChange={(e) => set('odemeTarihi', e.target.value)} required />
            </Field>
          </div>
          {form.odemeSekli === 'HAVALE_EFT' ? <HesapSecimi value={form.hesapId} onChange={(v) => set('hesapId', v)} /> : null}
          <Field label="Not (isteğe bağlı)">
            <Input value={form.not} onChange={(e) => set('not', e.target.value)} maxLength={255} />
          </Field>

          <div className="flex gap-2 pt-1">
            <Button type="submit" variant="primary" className="flex-1" disabled={kaydediyor || cakisma || sahalar.length === 0}>
              {kaydediyor ? 'Kaydediliyor…' : 'Aboneliği kaydet'}
            </Button>
            <Button type="button" variant="danger" onClick={() => navigate('/aboneler')}>Vazgeç</Button>
          </div>
          {sahalar.length === 0 ? <p className="m-0 text-[13.5px] text-yellow-800">Abonelik için önce aktif bir saha tanımlanmalıdır.</p> : null}
        </Card>
      </form>
    </div>
  );
}
