import { useEffect, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Field, Input, Select } from '@/components/ui/input';
import { ConfirmDialog } from '@/components/ui/dialog';
import HesapSecimi from '@/components/HesapSecimi';
import { iadeService } from '@/services';
import { apiHata } from '@/lib/api';
import { useToast } from '@/context/ToastContext';
import { useAuth } from '@/context/AuthContext';
import { kisaTarih, ODEME_YONTEMI_LABEL, tl, toIsoDate, tutarSinirla } from '@/lib/format';

const BUGUN = toIsoDate(new Date());

/**
 * Alınan ödemenin iadesi: yapılan iadeleri listeler, yönetici yeni iade girebilir ya da hatalı iadeyi silebilir.
 * İade, ödeme kaydını ve ödeme durumunu değiştirmez; raporlarda iade tarihinde tahsilattan düşülür.
 *
 * listeParams: iadeleri getiren sorgu parametreleri ({ rezervasyonId } gibi).
 * kaynaklar: iade yapılabilecek kayıtlar [{ id, alan: 'rezervasyonId' | 'ogrenciOdemeId' | 'envanterSatisId' | 'abonelikId', etiket, odenen }];
 *   birden fazlaysa (öğrencinin aylık ödemeleri) hangisine iade yapılacağı seçilir.
 * Yenileme: listeParams ya da kaynaklar (odenen) değişince liste yeniden çekilir.
 */
export default function IadeBolumu({ listeParams, kaynaklar }) {
  const { isAdmin } = useAuth();
  const toast = useToast();
  const [iadeler, setIadeler] = useState([]);
  const [acik, setAcik] = useState(false);
  const [hata, setHata] = useState('');
  const [kaydediyor, setKaydediyor] = useState(false);
  const [silinecek, setSilinecek] = useState(null);
  const [form, setForm] = useState({ kaynakId: '', tutar: '', sekil: '', hesapId: '', tarih: BUGUN, neden: '' });

  const anahtar = JSON.stringify(listeParams) + '|' + kaynaklar.map((k) => k.id + ':' + k.odenen).join(',');

  const yukle = () =>
    iadeService
      .list(listeParams)
      .then(setIadeler)
      .catch(() => setIadeler([]));

  useEffect(() => {
    yukle();
  }, [anahtar]);

  const iadeEdilen = (kaynakId) => iadeler.filter((i) => i.kaynakId === kaynakId).reduce((t, i) => t + Number(i.tutar), 0);
  const kalanOf = (k) => Math.max(Number(k.odenen) - iadeEdilen(k.id), 0);
  const secenekler = kaynaklar.filter((k) => kalanOf(k) > 0);
  const kaynakEtiketi = (id) => kaynaklar.find((k) => k.id === id)?.etiket;
  const cokluKaynak = kaynaklar.length > 1;

  // Form açılınca varsayılan kaynak ve tutar (iade edilebilecek tamamı)
  const ac = () => {
    const ilk = secenekler[0];
    setForm({ kaynakId: ilk ? String(ilk.id) : '', tutar: ilk ? String(kalanOf(ilk)) : '', sekil: '', hesapId: '', tarih: BUGUN, neden: '' });
    setHata('');
    setAcik(true);
  };

  const seciliKaynak = secenekler.find((k) => String(k.id) === form.kaynakId);

  if (!isAdmin && iadeler.length === 0) return null;
  if (isAdmin && iadeler.length === 0 && secenekler.length === 0) return null;

  const kaydet = async (e) => {
    e.preventDefault();
    setHata('');
    if (!seciliKaynak) {
      setHata('İade edilecek ödemeyi seçiniz.');
      return;
    }
    if (!form.sekil) {
      setHata('Lütfen iade şeklini seçiniz.');
      return;
    }
    setKaydediyor(true);
    try {
      await iadeService.yap({
        [seciliKaynak.alan]: seciliKaynak.id,
        tutar: Number(String(form.tutar).replace(',', '.')),
        iadeSekli: form.sekil,
        hesapId: form.sekil === 'HAVALE_EFT' && form.hesapId ? Number(form.hesapId) : null,
        iadeTarihi: form.tarih || null,
        neden: form.neden.trim() || null,
      });
      setAcik(false);
      toast.basari('İade kaydedildi.');
      await yukle();
    } catch (err) {
      setHata(apiHata(err));
    } finally {
      setKaydediyor(false);
    }
  };

  const sil = async () => {
    const i = silinecek;
    setSilinecek(null);
    try {
      await iadeService.remove(i.id);
      toast.basari('İade kaydı silindi.');
      await yukle();
    } catch (err) {
      toast.hata(apiHata(err));
    }
  };

  const toplam = iadeler.reduce((t, i) => t + Number(i.tutar), 0);

  return (
    <div className="grid gap-2.5 border-t border-divider pt-3">
      <div className="flex flex-wrap items-center gap-2">
        <div className="font-heading text-[18px] font-semibold">İadeler</div>
        {toplam > 0 ? <span className="text-[14px] text-ink/60">Toplam iade {tl(toplam)}</span> : null}
        {isAdmin && secenekler.length > 0 && !acik ? (
          <Button variant="secondary" size="sm" className="ml-auto" onClick={ac}>İade yap</Button>
        ) : null}
      </div>

      {iadeler.length > 0 ? (
        <div className="flex flex-col border border-divider bg-bg">
          {iadeler.map((i) => (
            <div key={i.id} className="flex flex-wrap items-center gap-x-3 gap-y-1 border-b border-hairline px-3 py-2 text-[14px] last:border-b-0">
              <span className="text-ink/60">{kisaTarih(i.iadeTarihi)}</span>
              <b className="tabular-nums text-hata-800">−{tl(i.tutar)}</b>
              <span className="text-ink/70">{ODEME_YONTEMI_LABEL[i.iadeSekli]}{i.hesapAd ? ' (' + i.hesapAd + ')' : ''}</span>
              {cokluKaynak && kaynakEtiketi(i.kaynakId) ? <span className="text-ink/70">· {kaynakEtiketi(i.kaynakId)}</span> : null}
              {i.neden ? <span className="text-ink/55">· {i.neden}</span> : null}
              {isAdmin ? (
                <button
                  type="button"
                  onClick={() => setSilinecek(i)}
                  className="ml-auto cursor-pointer border-0 bg-transparent p-0 text-[13.5px] text-hata-800 underline"
                >
                  Sil
                </button>
              ) : null}
            </div>
          ))}
        </div>
      ) : null}

      {acik ? (
        <form onSubmit={kaydet} className="grid gap-3 border border-divider bg-surface/40 p-3">
          {cokluKaynak ? (
            <Field label="İade edilecek ödeme">
              <Select
                value={form.kaynakId}
                onChange={(e) => {
                  const k = secenekler.find((x) => String(x.id) === e.target.value);
                  setForm({ ...form, kaynakId: e.target.value, tutar: k ? String(kalanOf(k)) : '' });
                }}
              >
                {secenekler.map((k) => (
                  <option key={k.id} value={k.id}>{k.etiket} · {tl(kalanOf(k))} iade edilebilir</option>
                ))}
              </Select>
            </Field>
          ) : null}
          <div className="grid grid-cols-2 gap-3">
            <Field label="İade tutarı (₺)">
              <Input
                type="number"
                min="0.01"
                step="0.01"
                inputMode="decimal"
                value={form.tutar}
                onChange={(e) => setForm({ ...form, tutar: tutarSinirla(e.target.value, seciliKaynak ? kalanOf(seciliKaynak) : 10000000) })}
                required
              />
            </Field>
            <Field label="İade tarihi">
              <Input type="date" max={BUGUN} value={form.tarih} onChange={(e) => setForm({ ...form, tarih: e.target.value })} required />
            </Field>
          </div>
          <Field label="İade şekli">
            <Select
              value={form.sekil}
              onChange={(e) => setForm({ ...form, sekil: e.target.value, hesapId: e.target.value === 'HAVALE_EFT' ? form.hesapId : '' })}
              required
            >
              <option value="">Seçiniz</option>
              {Object.entries(ODEME_YONTEMI_LABEL).map(([kod, ad]) => (
                <option key={kod} value={kod}>{ad}</option>
              ))}
            </Select>
          </Field>
          {form.sekil === 'HAVALE_EFT' ? <HesapSecimi value={form.hesapId} onChange={(v) => setForm({ ...form, hesapId: v })} label="İadenin yapıldığı hesap" /> : null}
          <Field label="Neden (isteğe bağlı)">
            <Input value={form.neden} onChange={(e) => setForm({ ...form, neden: e.target.value })} maxLength={255} />
          </Field>
          {hata ? <div role="alert" className="border border-hata-600 bg-hata-100 px-3 py-2 text-[14px] text-hata-800">{hata}</div> : null}
          <p className="m-0 text-[13px] text-ink/55">
            İade, alınan ödemeyi silmez; raporlarda iade tarihinde tahsilattan düşülür. Yanlış girilen bir ödemeyi silmek için ödeme satırındaki Sil'i kullanın.
          </p>
          <div className="flex gap-2">
            <Button type="submit" variant="primary" className="flex-1" disabled={kaydediyor}>{kaydediyor ? 'Kaydediliyor…' : 'İadeyi kaydet'}</Button>
            <Button type="button" variant="danger" onClick={() => setAcik(false)}>Vazgeç</Button>
          </div>
        </form>
      ) : null}

      <ConfirmDialog
        open={!!silinecek}
        title="İade kaydı silinsin mi?"
        description="Para gerçekte iade edilmediyse (hatalı kayıt) silin. İade edilen tutar tekrar iade edilebilir olur ve raporlardaki tahsilat artar."
        isim={silinecek ? tl(silinecek.tutar) + ' · ' + kisaTarih(silinecek.iadeTarihi) : ''}
        confirmLabel="Sil"
        tehlikeli
        onConfirm={sil}
        onCancel={() => setSilinecek(null)}
      />
    </div>
  );
}
