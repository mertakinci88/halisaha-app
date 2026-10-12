import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Field, Input } from '@/components/ui/input';
import { Seg, SegOpt } from '@/components/ui/seg';
import { ibanMaske } from '@/lib/format';

/** Hesap kayıt/güncelleme formu; onKaydet({ ad, iban, hesapSahibi, durum }) çağrılır. */
export default function HesapFormu({ baslangic, kaydediyor, onKaydet, kaydetEtiket }) {
  const navigate = useNavigate();
  const [form, setForm] = useState(baslangic);
  const set = (k, v) => setForm((p) => ({ ...p, [k]: v }));

  const gonder = (e) => {
    e.preventDefault();
    onKaydet({
      ad: form.ad.trim(),
      iban: form.iban,
      hesapSahibi: form.hesapSahibi.trim(),
      durum: form.durum,
    });
  };

  return (
    <form onSubmit={gonder} className="max-w-[560px]">
      <Card className="gap-3.5">
        <h4 className="text-[22px]">Hesap bilgileri</h4>

        <Field label="Hesap adı">
          <Input
            type="text"
            placeholder="Örn. Garanti Bankası X Şubesi"
            value={form.ad}
            onChange={(e) => set('ad', e.target.value)}
            required
          />
        </Field>

        <Field label="IBAN">
          <Input
            type="text"
            inputMode="text"
            autoComplete="off"
            placeholder="TR00 0000 0000 0000 0000 0000 00"
            value={ibanMaske(form.iban)}
            onChange={(e) => set('iban', e.target.value)}
            required
          />
        </Field>

        <Field label="Hesap sahibi (ad soyad)">
          <Input
            type="text"
            value={form.hesapSahibi}
            onChange={(e) => set('hesapSahibi', e.target.value)}
            required
          />
        </Field>

        <Field label="Durum">
          <Seg className="w-full">
            <SegOpt active={form.durum === 'AKTIF'} onClick={() => set('durum', 'AKTIF')} className="flex-1">
              Aktif
            </SegOpt>
            <SegOpt active={form.durum === 'PASIF'} onClick={() => set('durum', 'PASIF')} className="flex-1">
              Pasif
            </SegOpt>
          </Seg>
          <p className="m-0 mt-1.5 text-[13px] text-ink/55">
            Pasif hesaplar Havale / EFT hesabı seçimlerinde listelenmez; eski kayıtlarda adı görünmeye devam eder.
          </p>
        </Field>

        <div className="mt-1 flex gap-2">
          <Button type="submit" variant="primary" className="flex-1" disabled={kaydediyor}>
            {kaydediyor ? 'Kaydediliyor…' : kaydetEtiket}
          </Button>
          <Button variant="secondary" onClick={() => navigate('/hesaplar')}>
            Vazgeç
          </Button>
        </div>
      </Card>
    </form>
  );
}
