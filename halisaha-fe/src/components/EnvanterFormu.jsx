import { useState } from 'react';
import { Save, X } from 'lucide-react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Field, Input, Select } from '@/components/ui/input';
import { useEnvanterTurleri } from '@/lib/envanter';

const YENI_TUR = '__yeni';

/**
 * Yeni envanter ve envanter güncelleme formu. Envanter ve beden güncellemede de değiştirilebilir
 * (satışta kullanılmıyorsa); beden yalnızca bedenli türlerde sorulur ve türün kendi bedenlerinden seçilir, diğerlerinde
 * STANDART tutulur. Listede olmayan tür "Yeni envanter türü ekle" ile buradan eklenebilir.
 */
export default function EnvanterFormu({ baslangic, kaydetEtiketi, kaydediyor, onKaydet }) {
  const navigate = useNavigate();
  const { state, pathname } = useLocation();
  // Yeni tür sayfasından dönüldüyse yarım kalan form geri gelir ve yeni tür seçili olur
  const [form, setForm] = useState(() => {
    const temel = state?.form || baslangic || { turId: '', beden: '', satisFiyati: '', adet: '', stokUyariAdedi: '10' };
    return state?.turId ? { ...temel, turId: String(state.turId), beden: '' } : temel;
  });
  const { turler } = useEnvanterTurleri();
  const set = (k, v) => setForm((p) => ({ ...p, [k]: v }));
  const tur = (turler || []).find((t) => String(t.id) === String(form.turId));
  const bedenli = tur?.bedenli;

  const turDegisti = (deger) => {
    if (deger === YENI_TUR) return navigate('/envanter/tur/yeni', { state: { donus: pathname, form } });
    const yeni = (turler || []).find((t) => String(t.id) === deger);
    // Beden, yeni türün bedenlerinde yoksa sıfırlanır
    setForm((p) => ({ ...p, turId: deger, beden: yeni?.bedenli && yeni.bedenler.includes(p.beden) ? p.beden : '' }));
  };

  const gonder = (e) => {
    e.preventDefault();
    onKaydet({
      turId: Number(form.turId),
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
            <Select value={form.turId} onChange={(e) => turDegisti(e.target.value)} required className="text-[16px]">
              <option value="">Envanter seçin…</option>
              {(turler || []).map((t) => (
                <option key={t.id} value={t.id}>
                  {t.ad}
                </option>
              ))}
              <option value={YENI_TUR}>+ Yeni envanter türü ekle…</option>
            </Select>
          </Field>
          {bedenli ? (
            <Field label="Beden">
              <Select value={form.beden} onChange={(e) => set('beden', e.target.value)} required className="text-[16px]">
                <option value="">Beden seçin…</option>
                {(tur?.bedenler || []).map((b) => (
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
          <Field label={tur?.birim === 'çift' ? 'Çift' : 'Adet'}>
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
