import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Save, TriangleAlert, X } from 'lucide-react';
import { PageHeader } from '@/components/AppLayout';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Field, Input, Select, TelInput } from '@/components/ui/input';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import HesapSecimi from '@/components/HesapSecimi';
import { aboneService, sahaService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata, useToast } from '@/context/ToastContext';
import { kisaTarih, ODEME_YONTEMI_LABEL, tl, toIsoDate, tutarSinirla } from '@/lib/format';
import { BASLANGIC_SECENEKLERI, EN_UZUN_SURE_SAAT, HAFTA_GUNLERI, KAPANIS_DAKIKA, abonelikBitis, dakikaSaat, gunAdi, haftaGunuOf } from '@/lib/abone';

const BUGUN = toIsoDate(new Date());

export default function AboneKayitPage() {
  const navigate = useNavigate();
  const toast = useToast();
  const [hata, setHata] = useHata();
  const [sahalar, setSahalar] = useState(null);
  const [kaydediyor, setKaydediyor] = useState(false);
  const [onizleme, setOnizleme] = useState(null);
  const [odenenElle, setOdenenElle] = useState(false);
  const [haftalikElle, setHaftalikElle] = useState(false);
  const [gunElle, setGunElle] = useState(false);
  const [standart, setStandart] = useState(null);
  const [form, setForm] = useState({
    adSoyad: '',
    telNo: '',
    sahaId: '',
    haftaGunu: haftaGunuOf(BUGUN),
    baslangicDakika: 19 * 60,
    sureDakika: 60,
    baslangic: BUGUN,
    depozito: '',
    haftalikUcret: '',
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
          // Depozito standart tutardan dolar; elle değiştirilebilir
          depozito: ayar.standartUcret != null ? String(ayar.standartUcret) : p.depozito,
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

  // Alınan ödeme, elle değiştirilmediyse depozitoyla birlikte değişir (varsayılan: depozitonun tamamı)
  useEffect(() => {
    if (!odenenElle) set('odenen', form.depozito);
  }, [form.depozito, odenenElle]);

  // Haftalık ücret, elle değiştirilmediyse sahanın saatlik ücreti x süre olarak gelir (rezervasyondaki gibi)
  const seciliSaha = sahalar?.find((x) => String(x.id) === String(form.sahaId));
  useEffect(() => {
    if (haftalikElle) return;
    const saatlik = Number(seciliSaha?.saatlikUcret);
    set('haftalikUcret', saatlik > 0 ? String(Math.round(saatlik * (Number(form.sureDakika) / 60) * 100) / 100) : '');
  }, [seciliSaha, form.sureDakika, haftalikElle]);

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
          baslangicDakika: Number(form.baslangicDakika),
          sureDakika: Number(form.sureDakika),
          baslangicTarih: form.baslangic,
        })
        .then((o) => !iptal && setOnizleme(o))
        .catch(() => !iptal && setOnizleme(null));
    }, 400);
    return () => {
      iptal = true;
      clearTimeout(t);
    };
  }, [form.sahaId, form.haftaGunu, form.baslangicDakika, form.sureDakika, form.baslangic]);

  // Süre tam saat olarak seçilir; kapanışı (02:00) aşamaz
  const enUzunSaat = Math.min(EN_UZUN_SURE_SAAT, Math.floor((KAPANIS_DAKIKA - Number(form.baslangicDakika)) / 60));
  const bitis = onizleme?.bitisTarih || abonelikBitis(form.baslangic, form.haftaGunu);
  const depozito = Number(String(form.depozito).replace(',', '.')) || 0;
  const haftalik = Number(String(form.haftalikUcret).replace(',', '.')) || 0;
  const odenen = Number(String(form.odenen).replace(',', '.')) || 0;
  const cakisma = onizleme?.cakismalar?.length > 0;

  const kaydet = async (e) => {
    e.preventDefault();
    setHata('');
    if (!/^5\d{9}$/.test(form.telNo)) {
      setHata('Telefon numarası 10 haneli olmalı ve 5 ile başlamalıdır (örn. 546 976 46 13).');
      return;
    }
    if (!(depozito > 0)) {
      setHata('Depozito tutarını giriniz.');
      return;
    }
    if (!(haftalik > 0)) {
      setHata('Haftalık maç ücretini giriniz.');
      return;
    }
    if (!(odenen > 0)) {
      setHata('Alınan depozito tutarını giriniz.');
      return;
    }
    if (odenen > depozito) {
      setHata('Alınan ödeme depozitoyu (' + tl(depozito) + ') aşamaz.');
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
        baslangicDakika: Number(form.baslangicDakika),
        sureDakika: Number(form.sureDakika),
        baslangicTarih: form.baslangic,
        depozito,
        haftalikUcret: haftalik,
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
            <TelInput value={form.telNo} onChange={(v) => set('telNo', v)} required />
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
              <Input type="text" value={kisaTarih(bitis)} readOnly disabled aria-readonly="true" title="52. haftanın tarihi; otomatik hesaplanır" />
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
                value={form.baslangicDakika}
                onChange={(e) => {
                  const dakika = Number(e.target.value);
                  set('baslangicDakika', dakika);
                  // Seçilen saatte süre kapanışı aşıyorsa en uzun uygun süreye çekilir
                  const sinir = Math.floor((KAPANIS_DAKIKA - dakika) / 60) * 60;
                  if (form.sureDakika > sinir) set('sureDakika', Math.max(60, sinir));
                }}
              >
                {BASLANGIC_SECENEKLERI.map((d) => (
                  <option key={d} value={d}>{dakikaSaat(d)}</option>
                ))}
              </Select>
            </Field>
            <Field label="Süre">
              <Select value={form.sureDakika} onChange={(e) => set('sureDakika', Number(e.target.value))}>
                {Array.from({ length: Math.max(1, enUzunSaat) }, (_, i) => (i + 1) * 60).map((d) => (
                  <option key={d} value={d}>{d / 60} saat</option>
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
          <h4 className="text-[22px]">Depozito ve ödeme</h4>
          <Field label="Depozito (₺)">
            <Input
              type="number"
              min="0.01"
              step="0.01"
              inputMode="decimal"
              value={form.depozito}
              onChange={(e) => {
                set('depozito', tutarSinirla(e.target.value, 10000000));
              }}
              required
            />
            <p className="m-0 mt-1 text-[13px] text-ink/55">
              {standart != null
                ? 'Standart depozito ' + tl(standart) + '; gerekirse bu abone için değiştirebilirsiniz.'
                : 'Standart depozito tanımlı değil; Aboneler sayfasından yönetici tanımlayabilir.'}{' '}
              Depozito işletmede kalır; abonelik iptal edilince kalan haftalar bununla oynanır.
            </p>
          </Field>
          <Field label="Haftalık maç ücreti (₺)">
            <Input
              type="number"
              min="0.01"
              step="0.01"
              inputMode="decimal"
              value={form.haftalikUcret}
              onChange={(e) => {
                setHaftalikElle(true);
                set('haftalikUcret', tutarSinirla(e.target.value, 10000000));
              }}
              required
            />
            <p className="m-0 mt-1 text-[13px] text-ink/55">
              Sahanın ücretinden gelir, isterseniz değiştirebilirsiniz. Abone her hafta geldiğinde bu tutarı öder.
            </p>
          </Field>
          <Field label="Alınan depozito (₺)">
            <Input
              type="number"
              min="0.01"
              step="0.01"
              inputMode="decimal"
              value={form.odenen}
              onChange={(e) => {
                setOdenenElle(true);
                set('odenen', tutarSinirla(e.target.value, depozito || 10000000));
              }}
              required
            />
            <p className="m-0 mt-1 text-[13px] text-ink/55">
              Depozitonun tamamı ya da bir kısmı alınabilir; kalanı sonradan abone detayından tahsil edilir.
              {depozito > 0 && odenen > 0 && odenen < depozito ? ' Kalan: ' + tl(depozito - odenen) + '.' : ''}
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
              <Save size={16} strokeWidth={1.5} />
              <span>{kaydediyor ? 'Kaydediliyor…' : 'Aboneliği kaydet'}</span>
            </Button>
            <Button type="button" variant="cerceveHata" onClick={() => navigate('/aboneler')}>
              <X size={16} strokeWidth={1.5} />
              <span>Vazgeç</span>
            </Button>
          </div>
          {sahalar.length === 0 ? <p className="m-0 text-[13.5px] text-yellow-800">Abonelik için önce aktif bir saha tanımlanmalıdır.</p> : null}
        </Card>
      </form>
    </div>
  );
}
