import { cva } from 'class-variance-authority';
import { cn } from '@/lib/utils';

const buttonVariants = cva(
  'inline-flex items-center justify-center gap-2 rounded-none border font-heading font-semibold text-[15.5px] leading-tight cursor-pointer transition-colors disabled:opacity-45 disabled:cursor-not-allowed',
  {
    variants: {
      variant: {
        primary: 'bg-accent border-accent text-bg hover:bg-accent-600 active:bg-accent-700',
        secondary: 'bg-transparent border-divider text-ink hover:bg-ink/7 active:bg-ink/14',
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
