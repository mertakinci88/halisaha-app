import { useState } from 'react';
import { Save, X } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Field, Input, Select } from '@/components/ui/input';
import { BEDENLER, ENVANTER_TURLERI, turBul } from '@/lib/envanter';

/**
 * Yeni envanter ve envanter güncelleme formu. Envanter ve beden güncellemede de değiştirilebilir
 * (satışta kullanılmıyorsa); beden yalnızca bedenli türlerde sorulur, diğerlerinde STANDART tutulur.
 */
export default function EnvanterFormu({ baslangic, kaydetEtiketi, kaydediyor, onKaydet }) {
  const navigate = useNavigate();
  const [form, setForm] = useState(
    baslangic || { tur: '', beden: '', satisFiyati: '', adet: '', stokUyariAdedi: '10' },
  );
  const set = (k, v) => setForm((p) => ({ ...p, [k]: v }));
  const bedenli = turBul(form.tur)?.bedenli;

  const turDegisti = (tur) => setForm((p) => ({ ...p, tur, beden: turBul(tur)?.bedenli ? p.beden : '' }));

  const gonder = (e) => {
    e.preventDefault();
    onKaydet({
      tur: form.tur,
      beden: bedenli ? form.beden : null,
      satisFiyati: Number(form.satisFiyati),
      adet: Number(form.adet),
      stokUyariAdedi: Number(form.stokUyariAdedi),
    });
  };

  return (
    <form onSubmit={gonder} className="max-w-[560px]">
      <Card className="gap-3.5">
        <h4 className="text-[22px]">Envanter bilgileri</h4>

        <div className={bedenli ? 'grid grid-cols-1 gap-3 sm:grid-cols-2' : ''}>
          <Field label="Envanter">
            <Select value={form.tur} onChange={(e) => turDegisti(e.target.value)} required className="text-[16px]">
              <option value="">Envanter seçin…</option>
              {ENVANTER_TURLERI.map((t) => (
                <option key={t.kod} value={t.kod}>
                  {t.ad}
                </option>
              ))}
            </Select>
          </Field>
          {bedenli ? (
            <Field label="Beden">
              <Select value={form.beden} onChange={(e) => set('beden', e.target.value)} required className="text-[16px]">
                <option value="">Beden seçin…</option>
                {BEDENLER.map((b) => (
                  <option key={b} value={b}>
                    {b}
                  </option>
                ))}
              </Select>
            </Field>
          ) : null}
        </div>

        <div className="grid grid-cols-2 gap-3">
          <Field label="Satış fiyatı">
            <Input
              type="number"
              min="1"
              step="1"
              placeholder="450"
              value={form.satisFiyati}
              onChange={(e) => set('satisFiyati', e.target.value)}
              required
              className="text-[16px]"
            />
          </Field>
          <Field label={form.tur === 'TOZLUK' ? 'Çift' : 'Adet'}>
            <Input
              type="number"
              min="0"
              step="1"
              placeholder="40"
              value={form.adet}
              onChange={(e) => set('adet', e.target.value)}
              required
              className="text-[16px]"
            />
          </Field>
        </div>

        <Field label="Stok uyarı adedi">
          <Input
            type="number"
            min="0"
            step="1"
            placeholder="10"
            value={form.stokUyariAdedi}
            onChange={(e) => set('stokUyariAdedi', e.target.value)}
            required
            className="text-[16px]"
          />
          <p className="m-0 mt-1 text-[13px] text-ink/55">
            Stok bu rakamdan azsa listede "Stok az" uyarısı verilir.
          </p>
        </Field>

        <div className="mt-1 flex gap-2">
          <Button type="submit" variant="primary" className="flex-1" disabled={kaydediyor}>
            <Save size={16} strokeWidth={1.5} />
            <span>{kaydediyor ? 'Kaydediliyor…' : kaydetEtiketi}</span>
          </Button>
          <Button variant="cerceveHata" onClick={() => navigate('/envanter')}>
            <X size={16} strokeWidth={1.5} />
            <span>Vazgeç</span>
          </Button>
        </div>
      </Card>
    </form>
  );
}
