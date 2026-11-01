import { useState } from 'react';
import { Plus, Save, X } from 'lucide-react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Field, Input, Select } from '@/components/ui/input';
import { Seg, SegOpt } from '@/components/ui/seg';
import { Tag } from '@/components/ui/tag';

/**
 * Yeni envanter türü ve tür güncelleme formu. Bedenli türde bedenler tek tek eklenir (Enter ya da Ekle);
 * liste sırası, envanter formundaki seçim sırasıdır. Envanter kaydı olan türde bedenli / bedensiz seçimi değiştirilemez.
 * Başka bir formdan (yeni envanter) gelindiyse Vazgeç oraya döner.
 */
export default function EnvanterTurFormu({ baslangic, kilitli, kaydetEtiketi, kaydediyor, onKaydet }) {
  const navigate = useNavigate();
  const { state } = useLocation();
  const [ad, setAd] = useState(baslangic?.ad || '');
  const [bedenli, setBedenli] = useState(baslangic ? baslangic.bedenli : true);
  const [birim, setBirim] = useState(baslangic?.birim || 'adet');
  const [bedenler, setBedenler] = useState(baslangic?.bedenler || []);
  const [yeniBeden, setYeniBeden] = useState('');
  const [bedenHatasi, setBedenHatasi] = useState('');

  const bedenEkle = () => {
    const b = yeniBeden.trim();
    if (!b) return;
    if (b.length > 10) return setBedenHatasi('Beden en fazla 10 karakter olabilir.');
    if (bedenler.some((x) => x.toLowerCase() === b.toLowerCase())) return setBedenHatasi('Bu beden zaten ekli: ' + b);
    setBedenHatasi('');
    setBedenler((p) => [...p, b]);
    setYeniBeden('');
  };

  const gonder = (e) => {
    e.preventDefault();
    // Yazılıp Ekle'ye basılmamış beden de kaydedilir
    const tumu = bedenli && yeniBeden.trim() ? [...bedenler, yeniBeden.trim()] : bedenler;
    if (bedenli && tumu.length === 0) return setBedenHatasi('En az bir beden ekleyin.');
    onKaydet({ ad: ad.trim(), bedenli, birim, bedenler: bedenli ? tumu : [] });
  };

  const vazgec = () => (state?.donus ? navigate(state.donus, { replace: true, state: { form: state.form } }) : navigate('/envanter?sekme=turler'));

  return (
    <form onSubmit={gonder} className="max-w-[560px]">
      <Card className="gap-3.5">
        <h4 className="text-[22px]">Envanter türü bilgileri</h4>

        <Field label="Envanter adı">
          <Input value={ad} onChange={(e) => setAd(e.target.value)} placeholder="Yazlık Forma" maxLength={60} required className="text-[16px]" />
        </Field>

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <Field label="Beden">
            <Seg className="w-full">
              <SegOpt active={bedenli} disabled={kilitli} className="flex-1" onClick={() => setBedenli(true)}>
                Bedenli
              </SegOpt>
              <SegOpt active={!bedenli} disabled={kilitli} className="flex-1" onClick={() => setBedenli(false)}>
                Bedensiz
              </SegOpt>
            </Seg>
          </Field>
          <Field label="Stok birimi">
            <Select value={birim} onChange={(e) => setBirim(e.target.value)} className="text-[16px]">
              <option value="adet">Adet</option>
              <option value="çift">Çift</option>
            </Select>
          </Field>
        </div>
        {kilitli ? (
          <p className="m-0 -mt-1.5 text-[13px] text-ink/55">Bu türde envanter kaydı olduğu için bedenli / bedensiz seçimi değiştirilemez.</p>
        ) : null}

        {bedenli ? (
          <Field label="Bedenler">
            <div className="flex gap-2">
              <Input
                value={yeniBeden}
                onChange={(e) => setYeniBeden(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    bedenEkle();
                  }
                }}
                placeholder="Örn. 128, S, M, 38"
                maxLength={10}
                className="text-[16px]"
              />
              <Button type="button" variant="cerceve" onClick={bedenEkle}>
                <Plus size={16} strokeWidth={1.5} />
                <span>Ekle</span>
              </Button>
            </div>
            {bedenHatasi ? <p className="m-0 mt-1 text-[13px] text-hata-600">{bedenHatasi}</p> : null}
            <div className="mt-2 flex flex-wrap gap-1.5">
              {bedenler.length === 0 ? <span className="text-[13px] text-ink/55">Henüz beden eklenmedi.</span> : null}
              {bedenler.map((b) => (
                <Tag key={b} tone="outline" className="gap-1.5 !pr-1.5">
                  {b}
                  <button
                    type="button"
                    aria-label={b + ' bedenini kaldır'}
                    className="grid cursor-pointer place-items-center text-ink/60 hover:text-ink"
                    onClick={() => setBedenler((p) => p.filter((x) => x !== b))}
                  >
                    <X size={13} strokeWidth={1.75} />
                  </button>
                </Tag>
              ))}
            </div>
          </Field>
        ) : null}

        <div className="mt-1 flex gap-2">
          <Button type="submit" variant="primary" className="flex-1" disabled={kaydediyor || !ad.trim()}>
            <Save size={16} strokeWidth={1.5} />
            <span>{kaydediyor ? 'Kaydediliyor…' : kaydetEtiketi}</span>
          </Button>
          <Button variant="cerceveHata" onClick={vazgec}>
            <X size={16} strokeWidth={1.5} />
            <span>Vazgeç</span>
          </Button>
        </div>
      </Card>
    </form>
  );
}
