import { useEffect, useState } from 'react';
import { Check, Layers, ListChecks } from 'lucide-react';
import { Seg, SegOpt } from '@/components/ui/seg';
import { grupService } from '@/services';

/**
 * Personelin hangi grupların öğrencilerini göreceği: "Tüm gruplar" (varsayılan) ya da yalnızca seçilen gruplar.
 * value = { kisitli: boolean, grupIdleri: number[] }
 */
export default function GrupYetkiSecici({ value, onChange }) {
  const [gruplar, setGruplar] = useState(null);

  useEffect(() => {
    grupService.list().then(setGruplar).catch(() => setGruplar([]));
  }, []);

  const togla = (id) =>
    onChange({
      ...value,
      grupIdleri: value.grupIdleri.includes(id) ? value.grupIdleri.filter((g) => g !== id) : [...value.grupIdleri, id],
    });

  return (
    <div className="grid gap-2.5">
      <Seg className="w-full">
        <SegOpt active={!value.kisitli} onClick={() => onChange({ kisitli: false, grupIdleri: [] })} className="flex-1">
          <Layers size={16} strokeWidth={1.5} />
          <span>Tüm gruplar</span>
        </SegOpt>
        <SegOpt active={value.kisitli} onClick={() => onChange({ ...value, kisitli: true })} className="flex-1">
          <ListChecks size={16} strokeWidth={1.5} />
          <span>Seçili gruplar</span>
        </SegOpt>
      </Seg>

      {value.kisitli ? (
        gruplar === null ? (
          <p className="m-0 text-[13.5px] text-ink/55">Gruplar yükleniyor…</p>
        ) : gruplar.length === 0 ? (
          <p className="m-0 text-[13.5px] text-ink/55">Henüz grup yok. Önce Gruplar menüsünden grup ekleyin.</p>
        ) : (
          <>
            <div className="flex flex-wrap gap-2">
              {gruplar.map((g) => (
                <SegOpt
                  key={g.id}
                  active={value.grupIdleri.includes(g.id)}
                  onClick={() => togla(g.id)}
                  className="border border-divider border-l!"
                >
                  {value.grupIdleri.includes(g.id) ? <Check size={16} strokeWidth={1.5} /> : <Layers size={16} strokeWidth={1.5} />}
                  <span>{g.ad}</span>
                </SegOpt>
              ))}
            </div>
            <p className="m-0 text-[13px] text-ink/55">
              {value.grupIdleri.length === 0
                ? 'En az bir grup seçin.'
                : 'Personel yalnızca seçilen grupların öğrencilerini görür ve onlar üzerinde işlem yapar.'}
            </p>
          </>
        )
      ) : (
        <p className="m-0 text-[13px] text-ink/55">Personel tüm grupların öğrencilerini görür.</p>
      )}
    </div>
  );
}
