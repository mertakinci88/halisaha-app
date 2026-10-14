import { cn } from '@/lib/utils';

export function Seg({ className, children }) {
  return (
    <div className={cn('inline-flex rounded-none border border-divider overflow-hidden', className)}>
      {children}
    </div>
  );
}

const SECILI = {
  basari: 'bg-basari-600 text-white',
  notr: 'bg-neutral-600 text-white',
};

/** tone verilirse seçili seçenek o renkte dolar (basari = yeşil, notr = gri); verilmezse ana lacivert */
export function SegOpt({ active, tone, className, children, ...props }) {
  return (
    <button
      type="button"
      className={cn(
        'inline-flex items-center justify-center gap-1.5 px-3.5 py-[9px] text-[14.5px] cursor-pointer border-l border-divider first:border-l-0',
        active ? (SECILI[tone] || 'bg-accent text-bg') : 'bg-transparent text-ink hover:bg-ink/7',
        className,
      )}
      {...props}
    >
      {children}
    </button>
  );
}
