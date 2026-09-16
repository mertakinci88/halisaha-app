import { cn } from '@/lib/utils';

/** Industry: kart = şeffaf çizgi çizimi + 4 nişangah işareti */
export function Card({ className, children, ...props }) {
  return (
    <div className={cn('blueprint flex flex-col gap-3 p-5', className)} {...props}>
      <i className="corner tl" />
      <i className="corner tr" />
      <i className="corner bl" />
      <i className="corner br" />
      {children}
    </div>
  );
}

export function CardKicker({ className, children }) {
  return (
    <div className={cn('text-[11.5px] tracking-[0.1em] uppercase text-accent-700', className)}>{children}</div>
  );
}

export function CardTitle({ className, children }) {
  return <h4 className={cn('font-heading text-[22px]', className)}>{children}</h4>;
}
