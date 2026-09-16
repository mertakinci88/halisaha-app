import { cn } from '@/lib/utils';

const base =
  'w-full min-h-10 rounded-none bg-surface border border-divider px-[11px] py-2 text-[15.5px] text-ink font-body caret-accent hover:border-ink/45 focus-visible:border-accent focus-visible:outline-offset-0';

export function Input({ className, ...props }) {
  return <input className={cn(base, className)} {...props} />;
}

export function Select({ className, children, ...props }) {
  return (
    <select className={cn(base, 'appearance-none', className)} {...props}>
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
    <div className={className}>
      {label ? <Label>{label}</Label> : null}
      {children}
    </div>
  );
}
