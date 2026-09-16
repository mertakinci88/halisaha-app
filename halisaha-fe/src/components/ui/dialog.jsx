import { useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel = 'Onayla',
  cancelLabel = 'Vazgeç',
  onConfirm,
  onCancel,
  isim,
}) {
  useEffect(() => {
    if (!open) return;
    const onKeyDown = (e) => {
      if (e.key === 'Escape') onCancel?.();
    };
    document.addEventListener('keydown', onKeyDown);
    return () => document.removeEventListener('keydown', onKeyDown);
  }, [open, onCancel]);

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-50 grid place-items-center bg-ink/40 px-5"
      onClick={onCancel}
    >
      <div
        className={cn('blueprint w-full max-w-[380px] gap-4 bg-bg p-6 flex flex-col')}
        onClick={(e) => e.stopPropagation()}
      >
        <i className="corner tl" />
        <i className="corner tr" />
        <i className="corner bl" />
        <i className="corner br" />

        <div>
          <h4 className="text-[22px]">{title}</h4>
          {description ? <p className="m-0 mt-1 text-[14.5px] text-ink/55">{description}</p> : null}
          {isim ? <p className="m-0 mt-2 font-heading text-[17px]">{isim}</p> : null}
        </div>

        <div className="flex gap-2">
          <Button variant="secondary" className="flex-1" onClick={onCancel}>
            {cancelLabel}
          </Button>
          <Button variant="primary" className="flex-1" onClick={onConfirm}>
            {confirmLabel}
          </Button>
        </div>
      </div>
    </div>
  );
}
