import { SegOpt } from '@/components/ui/seg';
import { MODUL_LABEL, ModulIkon, OGRENCI_ISLEM_YETKILERI } from '@/lib/yetki';

/** Öğrenci detayında hangi kartların görüneceği ve işlem yapılabileceği (transfer / ödeme / envanter). */
export default function OgrenciIslemYetkileri({ yetkiler, onTogla }) {
  return (
    <div className="flex flex-wrap gap-2">
      {OGRENCI_ISLEM_YETKILERI.map((m) => (
        <SegOpt
          key={m}
          active={yetkiler.includes(m)}
          onClick={() => onTogla(m)}
          className="border border-divider border-l!"
        >
          <ModulIkon modul={m} />
          <span>{MODUL_LABEL[m]}</span>
        </SegOpt>
      ))}
    </div>
  );
}
