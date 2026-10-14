import { Phone } from 'lucide-react';
import { cn } from '@/lib/utils';
import { telMaske, telRakam } from '@/lib/format';

const base =
  'w-full min-w-0 max-w-full min-h-10 rounded-none bg-surface border border-divider px-[11px] py-2 text-[15.5px] text-ink font-body caret-accent hover:border-ink/45 focus-visible:border-accent focus-visible:outline-offset-0';

export function Input({ className, ...props }) {
  return <input className={cn(base, className)} {...props} />;
}

export function Select({ className, children, ...props }) {
  return (
    <select className={cn(base, 'appearance-none select-combo', className)} {...props}>
      {children}
    </select>
  );
}

export function Textarea({ className, ...props }) {
  return <textarea className={cn(base, 'min-h-24 resize-y', className)} {...props} />;
}

export function Label({ className, children, ...props }) {
  return (
    <label className={cn('block text-[13.5px] text-ink/70 mb-1', className)} {...props}>
      {children}
    </label>
  );
}

export function Field({ label, children, className }) {
  return (
    <div className={cn('min-w-0', className)}>
      {label ? <Label>{label}</Label> : null}
      {children}
    </div>
  );
}

/** Telefon girişi: tek çerçeve, telefon ikonu + "+90" ön eki + maskeli numara. value = ham rakamlar (5469764613). */
export function TelInput({ value, onChange, className, ...props }) {
  return (
    <div
      className={cn(
        'flex min-h-10 w-full min-w-0 items-stretch border border-divider bg-surface hover:border-ink/45 focus-within:border-accent focus-within:outline focus-within:outline-2 focus-within:outline-accent',
        className,
      )}
    >
      <span className="flex items-center gap-1.5 border-r border-divider bg-ink/5 px-3 text-[15px] font-medium text-ink/70">
        <Phone size={15} strokeWidth={1.5} />
        <span>+90</span>
      </span>
      <input
        type="tel"
        inputMode="numeric"
        autoComplete="tel-national"
        placeholder="5__ ___ __ __"
        value={telMaske(value)}
        onChange={(e) => onChange(telRakam(e.target.value))}
        className="min-w-0 flex-1 border-0 bg-transparent px-[11px] py-2 font-body text-[16px] tracking-wide tabular-nums text-ink caret-accent outline-none focus-visible:outline-none"
        {...props}
      />
    </div>
  );
}
