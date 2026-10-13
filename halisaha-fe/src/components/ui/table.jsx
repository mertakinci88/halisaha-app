import { cn } from '@/lib/utils';

/**
 * kart: dar ekranda (sm altı) tablo kart listesine dönüşür (başlık gizlenir, her satır bir kart olur,
 * hücreler Td'deki `etiket` ile alt alta dizilir; `tam` hücre tüm genişliği kaplar). Masaüstü aynı kalır.
 */
export function Table({ className, kart = false, children }) {
  return (
    <div className="w-full overflow-x-auto">
      <table className={cn('w-full border-collapse text-[15.5px]', kart && 'tablo-kart', className)}>{children}</table>
    </div>
  );
}

export function Th({ className, children, ...props }) {
  return (
    <th
      className={cn(
        'text-left text-[12.5px] tracking-[0.08em] uppercase text-ink/60 font-normal px-2 py-2 border-b border-divider whitespace-nowrap',
        className,
      )}
      {...props}
    >
      {children}
    </th>
  );
}

export function Td({ className, etiket, tam, children, ...props }) {
  return (
    <td
      className={cn('px-2 py-2 border-b border-hairline align-middle', className)}
      data-etiket={etiket}
      data-tam={tam ? '' : undefined}
      {...props}
    >
      {children}
    </td>
  );
}

export function Tr({ className, children, ...props }) {
  return (
    <tr className={cn('hover:bg-ink/4', className)} {...props}>
      {children}
    </tr>
  );
}

export function Bos({ colSpan, children = 'Kayıt bulunamadı.' }) {
  return (
    <tr>
      <td colSpan={colSpan} className="px-2 py-6 text-center text-ink/55">
        {children}
      </td>
    </tr>
  );
}
