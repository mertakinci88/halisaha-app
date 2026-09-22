import { cn } from '@/lib/utils';

const tones = {
  accent: 'bg-accent-100 text-accent-800',
  neutral: 'bg-neutral-100 text-neutral-800',
  outline: 'border border-accent text-accent-700',
  danger: 'bg-red-100 text-red-800',
  warning: 'bg-yellow-100 text-yellow-800',
};

export function Tag({ tone = 'neutral', className, children }) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-none px-[11px] py-1 text-[12.5px] tracking-[0.02em] whitespace-nowrap',
        tones[tone],
        className,
      )}
    >
      {children}
    </span>
  );
}
