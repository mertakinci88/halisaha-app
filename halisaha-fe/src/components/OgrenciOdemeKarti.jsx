import { useEffect, useRef, useState } from 'react';
import { AlertTriangle, ChevronLeft, ChevronRight, CornerDownRight, Pencil, Plus, Save, Trash2, X } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Field, Input, Select } from '@/components/ui/input';
import { Seg, SegOpt } from '@/components/ui/seg';
import { ConfirmDialog } from '@/components/ui/dialog';
import OdemeDurumuEtiketi from '@/components/OdemeDurumuEtiketi';
import IadeBolumu from '@/components/IadeBolumu';
import { Hata, Yukleniyor } from '@/components/ui/durum';
import { hesapService, ogrenciOdemeService } from '@/services';
import { apiHata } from '@/lib/api';
import { useHata, useToast } from '@/context/ToastContext';
import { AYLAR, kisaTarih, ODEME_YONTEMI_LABEL, toIsoDate, tl } from '@/lib/format';
import { cn } from '@/lib/utils';

const SATIR_RENK = {
  ODENDI: 'bg-basari-100',
  KISMI_ODENDI: 'bg-yellow-100',
  KISMI_GECIKMIS: 'bg-hata-100',
  ODEME_BEKLIYOR: 'bg-yellow-100',
  ODENMEDI: 'bg-hata-100',
};

const KISA_DURUM = {
  ODENDI: 'Ödendi',
  KISMI_ODENDI: 'Kısmi',
  KISMI_GECIKMIS: 'Gecikmiş',
  ODEME_BEKLIYOR: 'Bekliyor',
  ODENMEDI: 'Ödenmedi',
};

const GECIKMIS = ['KISMI_GECIKMIS', 'ODENMEDI'];

const KART_CERCEVE = {
  ODENDI: 'border-basari-600',
  KISMI_ODENDI: 'border-yellow-600',
  KISMI_GECIKMIS: 'border-hata-600',
  ODEME_BEKLIYOR: 'border-yellow-600',
  ODENMEDI: 'border-hata-600',
};

const BOS_FORM = { id: null, tutar: '', sekil: '', hesapId: '', tarih: '', not: '', tamam: true };

const tutarSayi = (t) => (t === '' ? 0 : Number(t));

const formOf = (o) => ({
  id: o.id,
  tutar: Number(o.odenenTutar) > 0 ? String(o.odenenTutar) : '',
  sekil: o.odemeSekli || '',
  hesapId: o.hesapId != null ? String(o.hesapId) : '',
  tarih: o.odemeTarihi ? String(o.odemeTarihi).slice(0, 10) : '',
  not: o.not || '',
  tamam: o.tamam,
});

/**
 * Öğrenci detayındaki aylık ödemeler: seçili yılın 12 ayı için ödemeler listelenir. Bir aya birden fazla ödeme eklenebilir
 * (kısmi ödeme); her ödemede "Tamamı ödendi" ya da "Kısmi ödeme" seçilir. Tamamı ödendi işaretli ödeme olan ay Ödendi (yeşil),
 * yalnızca kısmi ödemesi olan ay Kısmi ödendi (sarı; ayın 15'inden sonra Gecikmiş, kırmızı), ödemesi olmayan ay 15'ine kadar
 * Ödeme bekliyor (sarı), sonra Ödenmedi (kırmızı) görünür. Kalan para başka bir ayda alınırsa ödeme borcu olan aya, o günün
 * tarihiyle girilir. Tutar boş + açıklama + "Tamamı ödendi" ayı parasız kapatır.
 */
export default function OgrenciOdemeKarti({ ogrenciId }) {
  const toast = useToast();
  const [hata, setHata] = useHata();
  const [yil, setYil] = useState(new Date().getFullYear());

  const [veri, setVeri] = useState(null);
  const [hesaplar, setHesaplar] = useState([]);
  const [yukleniyor, setYukleniyor] = useState(true);
  const [kaydediyor, setKaydediyor] = useState(false);
  const [acikAy, setAcikAy] = useState(null);
  const [seciliAy, setSeciliAy] = useState(null);
  const [form, setForm] = useState(BOS_FORM);
  const [formHata, setFormHata] = useState('');
  const [silinecek, setSilinecek] = useState(null);
  const formRef = useRef(null);
  const panelRef = useRef(null);

  const yukle = () => {
    setYukleniyor(true);
    setHata('');
    ogrenciOdemeService
      .getir(ogrenciId, yil)
      .then(setVeri)
      .catch((e) => setHata(apiHata(e), { sessiz: true }))
      .finally(() => setYukleniyor(false));
  };

  useEffect(() => {
    setAcikAy(null);
    setSeciliAy(yil === new Date().getFullYear() ? new Date().getMonth() + 1 : null);
    yukle();
  }, [ogrenciId, yil]);

  useEffect(() => {
    hesapService.list().then(setHesaplar).catch(() => setHesaplar([]));
  }, []);

  // Form açılınca (ekleme ya da düzenleme) ekranın ortasına getir
  useEffect(() => {
    if (acikAy != null) formRef.current?.scrollIntoView({ block: 'center', behavior: 'smooth' });
  }, [acikAy, form.id]);

  const hesapSecenekleri = (secili) => hesaplar.filter((h) => h.durum === 'AKTIF' || String(h.id) === String(secili));

  const ekleAc = (ay) => {
    setForm({ ...BOS_FORM, tarih: toIsoDate(new Date()) });
    setFormHata('');
    setAcikAy(ay);
  };
  const duzenleAc = (ay, odeme) => {
    setForm(formOf(odeme));
    setFormHata('');
    setAcikAy(ay);
  };
  const kapat = () => {
    setAcikAy(null);
    setFormHata('');
  };
  const sat = (kismi) => {
    setFormHata('');
    setForm((p) => ({ ...p, ...kismi }));
  };

  const tutar = tutarSayi(form.tutar);

  const kaydet = async () => {
    if (tutar < 0 || Number.isNaN(tutar)) return setFormHata('Geçerli bir tutar girin.');
    if (tutar === 0 && !form.not.trim()) return setFormHata('Tutar girin ya da parasız kayıt için açıklama yazın.');
    if (tutar > 0 && !form.sekil) return setFormHata('Ödeme yöntemini seçin.');
    if (tutar > 0 && form.sekil === 'HAVALE_EFT' && !form.hesapId) return setFormHata('Havale / EFT için hesap seçin.');
    const body = {
      odenenTutar: tutar,
      odemeSekli: tutar > 0 ? form.sekil : null,
      hesapId: tutar > 0 && form.sekil === 'HAVALE_EFT' ? Number(form.hesapId) : null,
      odemeTarihi: tutar > 0 ? form.tarih || null : null,
      not: form.not.trim() || null,
      tamam: form.tamam,
    };
    setKaydediyor(true);
    try {
      const v = form.id
        ? await ogrenciOdemeService.guncelle(ogrenciId, yil, acikAy, form.id, body)
        : await ogrenciOdemeService.ekle(ogrenciId, yil, acikAy, body);
      setVeri(v);
      toast.basari(form.id ? 'Ödeme güncellendi.' : 'Ödeme kaydedildi.');
      kapat();
    } catch (err) {
      setFormHata(apiHata(err));
    } finally {
      setKaydediyor(false);
    }
  };

  const sil = async () => {
    const { ay, odeme } = silinecek;
    setSilinecek(null);
    try {
      setVeri(await ogrenciOdemeService.sil(ogrenciId, yil, ay, odeme.id));
      toast.basari('Ödeme silindi.');
    } catch (err) {
      setHata(apiHata(err));
    }
  };

  const yillar = veri?.yillar || [yil];
  const yilIndex = yillar.indexOf(yil);
  const yilGit = (y) => {
    if (acikAy != null) return toast.hata('Önce açık ödeme formunu kaydedin ya da vazgeçin.');
    setYil(y);
  };
  const ayGit = (ay) => {
    if (acikAy != null && acikAy !== ay) return toast.hata('Önce açık ödeme formunu kaydedin ya da vazgeçin.');
    setSeciliAy(seciliAy === ay ? null : ay);
    if (seciliAy !== ay) setTimeout(() => panelRef.current?.scrollIntoView({ block: 'nearest', behavior: 'smooth' }), 0);
  };
  const odenenAy = veri ? veri.aylar.filter((a) => a.durum === 'ODENDI').length : 0;
  const gecikmisAy = veri ? veri.aylar.filter((a) => GECIKMIS.includes(a.durum)).length : 0;
  const buAyMi = (a) => veri != null && a.yil === Number(veri.bugun.slice(0, 4)) && a.ay === Number(veri.bugun.slice(5, 7));

  const formKutusu = (a) => (
    <div ref={formRef} className="mx-2 mb-3 mt-1 grid gap-3 border border-divider bg-bg p-3">
      <div className="text-[15px] font-semibold">
        {AYLAR[a.ay - 1]} {yil} · {form.id ? 'Ödemeyi düzenle' : 'Ödeme ekle'}
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        <Field label="Tutar (₺)">
          <Input
            type="number"
            min="0"
            step="50"
            inputMode="decimal"
            placeholder="0"
            value={form.tutar}
            onChange={(e) => sat({ tutar: e.target.value })}
            className="text-right tabular-nums"
          />
        </Field>
        <Field label="Ödeme tarihi">
          <Input type="date" disabled={tutar === 0} value={form.tarih} onChange={(e) => sat({ tarih: e.target.value })} />
        </Field>
        <Field label="Ödeme yöntemi">
          <Select
            disabled={tutar === 0}
            value={form.sekil}
            onChange={(e) => sat({ sekil: e.target.value, hesapId: e.target.value === 'HAVALE_EFT' ? form.hesapId : '' })}
          >
            <option value="">Seçiniz</option>
            <option value="NAKIT">Nakit</option>
            <option value="KART">Kart</option>
            <option value="HAVALE_EFT">Havale / EFT</option>
          </Select>
        </Field>
        {form.sekil === 'HAVALE_EFT' && tutar > 0 ? (
          <Field label="Hesap">
            <Select value={form.hesapId} onChange={(e) => sat({ hesapId: e.target.value })}>
              <option value="">Hesap seçin</option>
              {hesapSecenekleri(form.hesapId).map((h) => (
                <option key={h.id} value={h.id}>
                  {h.ad}
                  {h.durum === 'PASIF' ? ' (pasif)' : ''}
                </option>
              ))}
            </Select>
          </Field>
        ) : null}
      </div>
      <Field label="Not">
        <Input type="text" maxLength={255} placeholder="Not" value={form.not} onChange={(e) => sat({ not: e.target.value })} />
      </Field>
      <div>
        <div className="mb-1.5 text-[13.5px] text-ink/70">Bu ödeme ayı tamamlıyor mu?</div>
        <Seg>
          <SegOpt active={form.tamam} tone="basari" onClick={() => sat({ tamam: true })}>
            Tamamı ödendi
          </SegOpt>
          <SegOpt active={!form.tamam} onClick={() => sat({ tamam: false })}>
            Kısmi ödeme
          </SegOpt>
        </Seg>
        <p className="m-0 mt-1.5 text-[13px] text-ink/55">
          {form.tamam
            ? tutar === 0
              ? 'Tutar boş: ay para alınmadan kapatılır (indirim, burs vb.); açıklama yazın.'
              : 'Bu ödemeyle ay Ödendi olur.'
            : 'Ay açık kalır; kalan ödeme bu aya ayrıca eklenir (başka bir ayda alınsa da).'}
        </p>
      </div>
      {formHata ? (
        <div role="alert" className="border border-hata-600 bg-hata-100 px-3 py-2 text-[14px] text-hata-800">
          {formHata}
        </div>
      ) : null}
      <div className="flex flex-wrap gap-2">
        <Button variant="primary" disabled={kaydediyor} onClick={kaydet}>
          <Save size={16} strokeWidth={1.5} />
          <span>{kaydediyor ? 'Kaydediliyor…' : 'Kaydet'}</span>
        </Button>
        <Button variant="cerceveHata" disabled={kaydediyor} onClick={kapat}>
          <X size={16} strokeWidth={1.5} />
          <span>Vazgeç</span>
        </Button>
      </div>
    </div>
  );

  const odemeSatiri = (a, o) => (
    <li key={o.id} className="flex flex-wrap items-center gap-x-3 gap-y-1 border-t border-hairline/70 py-1.5 pl-3 pr-1 text-[14.5px]">
      <CornerDownRight size={16} strokeWidth={1.5} className="-ml-5 shrink-0 text-ink/45" aria-hidden="true" />
      <span className="tabular-nums text-ink/70">{o.odemeTarihi ? kisaTarih(o.odemeTarihi) : '—'}</span>
      <b className="font-semibold tabular-nums">{tl(o.odenenTutar)}</b>
      <span className="text-ink/70">
        {o.odemeSekli ? ODEME_YONTEMI_LABEL[o.odemeSekli] + (o.hesapAd ? ' · ' + o.hesapAd : '') : 'Para alınmadı'}
      </span>
      {o.tamam ? <span className="text-[13px] font-semibold text-basari-800">✓ ayı kapattı</span> : null}
      {o.not ? <span className="min-w-0 break-words text-ink/70">{o.not}</span> : null}
      <span className="ml-auto flex gap-1">
        <Button size="icon" variant="ghost" aria-label="Ödemeyi düzenle" onClick={() => duzenleAc(a.ay, o)}>
          <Pencil size={16} strokeWidth={1.5} />
        </Button>
        <Button size="icon" variant="ghost" aria-label="Ödemeyi sil" onClick={() => setSilinecek({ ay: a.ay, odeme: o })}>
          <Trash2 size={16} strokeWidth={1.5} />
        </Button>
      </span>
    </li>
  );

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
          <div className="grid gap-2 border-l-4 border-accent bg-accent-100 px-3 py-2">
            <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1">
              <span className="text-[15.5px] font-semibold text-ink/80">{yil} toplam ödenen:</span>
              <b className="font-heading text-[26px] font-semibold tabular-nums text-accent-700">{tl(veri.toplamOdenen)}</b>
            </div>
            <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
              <span className="text-[14.5px] font-semibold tabular-nums">{odenenAy}/12 ay ödendi</span>
              {gecikmisAy > 0 ? (
                <span className="inline-flex items-center gap-1 text-[14px] font-semibold text-hata-800">
                  <AlertTriangle size={15} strokeWidth={1.5} aria-hidden="true" />
                  {gecikmisAy} ay gecikmiş
                </span>
              ) : null}
            </div>
            <div className="h-1.5 w-full bg-white/70" role="progressbar" aria-valuemin={0} aria-valuemax={12} aria-valuenow={odenenAy} aria-label="Ödenen ay sayısı">
              <div className="h-full bg-basari-600" style={{ width: (odenenAy / 12) * 100 + '%' }} />
            </div>
          </div>

          <div className="grid grid-cols-3 gap-2 sm:grid-cols-4">
            {veri.aylar.map((a) => (
              <button
                key={a.ay}
                type="button"
                aria-pressed={seciliAy === a.ay}
                onClick={() => ayGit(a.ay)}
                className={cn(
                  'flex min-w-0 cursor-pointer flex-col items-start gap-0.5 border px-2.5 py-2 text-left transition-[box-shadow,filter] hover:shadow-md hover:brightness-95 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent-700',
                  SATIR_RENK[a.durum],
                  seciliAy === a.ay ? cn('border-2 border-accent-700 px-[9px] py-[7px]') : KART_CERCEVE[a.durum],
                )}
              >
                <span className="flex w-full items-center gap-1.5 font-semibold">
                  <span className="truncate">{AYLAR[a.ay - 1]}</span>
                  {buAyMi(a) ? <span className="text-[11.5px] font-normal text-ink/60">bu ay</span> : null}
                </span>
                <span className="inline-flex items-center gap-1 text-[12.5px] font-semibold text-ink/75">
                  {GECIKMIS.includes(a.durum) ? <AlertTriangle size={13} strokeWidth={1.5} className="shrink-0 text-hata-800" aria-hidden="true" /> : null}
                  {KISA_DURUM[a.durum] || '—'}
                </span>
                <b className="text-[14.5px] font-semibold tabular-nums">{a.odemeler.length > 0 ? tl(a.odenenTutar) : '—'}</b>
              </button>
            ))}
          </div>

          {(() => {
            const a = veri.aylar.find((x) => x.ay === seciliAy);
            if (!a) return <p className="m-0 text-[14px] text-ink/60">Ödemeleri görmek ya da eklemek için bir aya dokunun.</p>;
            return (
              <div ref={panelRef} className="border border-ink/35 bg-white">
                <div className={cn('flex flex-wrap items-center gap-x-3 gap-y-2 border-b border-hairline px-3 py-2.5', SATIR_RENK[a.durum])}>
                  <span className="text-[16.5px] font-semibold">
                    {AYLAR[a.ay - 1]} {yil}
                  </span>
                  <OdemeDurumuEtiketi belirgin durum={a.durum} />
                  {a.odemeler.length > 0 ? <b className="font-semibold tabular-nums">{tl(a.odenenTutar)}</b> : null}
                  <Button size="sm" variant="cerceve" className="ml-auto" disabled={acikAy != null} onClick={() => ekleAc(a.ay)}>
                    <Plus size={15} strokeWidth={1.5} />
                    <span>Ödeme ekle</span>
                  </Button>
                </div>
                {a.odemeler.length > 0 ? (
                  <ul className="m-0 list-none pb-1.5 pl-8 pr-2">
                    {a.odemeler.map((o) => (acikAy === a.ay && form.id === o.id ? null : odemeSatiri(a, o)))}
                  </ul>
                ) : (
                  <p className="m-0 px-3 py-3 text-[14.5px] text-ink/75">Bu ay için ödeme girilmemiş.</p>
                )}
                {acikAy === a.ay ? formKutusu(a) : null}
              </div>
            );
          })()}

          <IadeBolumu
            listeParams={{ ogrenciId, yil }}
            kaynaklar={veri.aylar.flatMap((a) =>
              a.odemeler
                .filter((o) => Number(o.odenenTutar) > 0)
                .map((o) => ({
                  id: o.id,
                  alan: 'ogrenciOdemeId',
                  etiket: AYLAR[a.ay - 1] + ' ' + yil + ' · ' + kisaTarih(o.odemeTarihi) + ' · ' + tl(o.odenenTutar),
                  odenen: Number(o.odenenTutar),
                })),
            )}
          />
        </>
      ) : null}

      <ConfirmDialog
        open={!!silinecek}
        title="Ödeme silinsin mi?"
        description="Ödeme kaydı silinir; ayın durumu kalan ödemelere göre yeniden hesaplanır."
        isim={silinecek ? AYLAR[silinecek.ay - 1] + ' · ' + tl(silinecek.odeme.odenenTutar) : ''}
        confirmLabel="Sil"
        tehlikeli
        onConfirm={sil}
        onCancel={() => setSilinecek(null)}
      />
    </Card>
  );
}
