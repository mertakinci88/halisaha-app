import { cva } from 'class-variance-authority';
import { cn } from '@/lib/utils';

const buttonVariants = cva(
  'inline-flex items-center justify-center gap-2 rounded-none border font-heading font-semibold text-[15.5px] leading-tight cursor-pointer transition-colors disabled:opacity-45 disabled:cursor-not-allowed',
  {
    variants: {
      variant: {
        primary: 'bg-accent border-accent text-bg hover:bg-accent-600 active:bg-accent-700',
        secondary: 'bg-transparent border-divider text-ink hover:bg-ink/7 active:bg-ink/14',
        guncelle: 'bg-accent border-accent text-bg hover:bg-accent-600 hover:border-accent-600 active:bg-accent-700',
        danger: 'bg-hata-600 border-hata-600 text-white hover:bg-hata-800 hover:border-hata-800 active:bg-hata-800',
        gri: 'bg-neutral-200 border-neutral-200 text-ink hover:bg-neutral-300 hover:border-neutral-300 active:bg-neutral-400',
        cerceve: 'bg-transparent border-accent text-accent hover:bg-accent/10 active:bg-accent/18',
        cerceveHata: 'bg-transparent border-hata-600 text-hata-600 hover:bg-hata-100 active:bg-hata-300/60',
        ghost: 'bg-transparent border-transparent text-accent-700 hover:bg-accent/10 active:bg-accent/18',
      },
      size: {
        default: 'px-[15px] py-[9px]',
        sm: 'px-3 py-1.5 text-[14px]',
        icon: 'h-9 w-9 p-0',
        block: 'w-full px-[15px] py-[11px]',
      },
    },
    defaultVariants: { variant: 'secondary', size: 'default' },
  },
);

export function Button({ className, variant, size, type = 'button', ...props }) {
  return <button type={type} className={cn(buttonVariants({ variant, size }), className)} {...props} />;
}

export { buttonVariants };
