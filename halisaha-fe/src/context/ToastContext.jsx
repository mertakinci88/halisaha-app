import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { CircleAlert, CircleCheck, X } from 'lucide-react';
import { cn } from '@/lib/utils';

const ToastContext = createContext(null);

const SURE = { basari: 4000, hata: 7000 };
const STIL = {
  basari: { kutu: 'border-basari-600 bg-basari-100 text-basari-800', ikon: 'text-basari-600', Ikon: CircleCheck },
  hata: { kutu: 'border-hata-600 bg-hata-100 text-hata-800', ikon: 'text-hata-600', Ikon: CircleAlert },
};

/**
 * Bildirimler: başarı yeşil, hata kırmızı. Sayfa başlığındaki (PageHeader) Takvim / Hızlı rezervasyon
 * butonlarının solundaki boşlukta, o satırın hizasında görünür; başlığı olmayan sayfalarda sağ üst köşede.
 */
export function ToastProvider({ children }) {
  const [liste, setListe] = useState([]);
  const [alan, setAlan] = useState(null);
  const sayac = useRef(0);

  const kapat = useCallback((id) => setListe((l) => l.filter((t) => t.id !== id)), []);

  const goster = useCallback(
    (tip, mesaj) => {
      if (!mesaj) return;
      const id = ++sayac.current;
      setListe((l) => [...l.slice(-3), { id, tip, mesaj }]);
      setTimeout(() => kapat(id), SURE[tip]);
    },
    [kapat],
  );

  const toast = useMemo(
    () => ({
      basari: (mesaj) => goster('basari', mesaj),
      hata: (mesaj) => goster('hata', mesaj),
      alanKaydet: setAlan,
    }),
    [goster],
  );

  const kutu = (
    <div
      className={cn(
        'pointer-events-none z-50 flex flex-col gap-2',
        alan
          ? 'max-sm:fixed max-sm:inset-x-4 max-sm:top-4 sm:absolute sm:right-0 sm:top-0 sm:w-96 sm:max-w-[calc(100vw-2.5rem)]'
          : 'fixed right-4 top-4 w-[calc(100vw-2rem)] max-w-sm',
      )}
      aria-live="polite"
    >
      {liste.map((t) => (
        <Bildirim key={t.id} tip={t.tip} mesaj={t.mesaj} onKapat={() => kapat(t.id)} />
      ))}
    </div>
  );

  return (
    <ToastContext.Provider value={toast}>
      {children}
      {alan ? createPortal(kutu, alan) : kutu}
    </ToastContext.Provider>
  );
}

function Bildirim({ tip, mesaj, onKapat }) {
  const { kutu, ikon, Ikon } = STIL[tip];
  return (
    <div
      role={tip === 'hata' ? 'alert' : 'status'}
      className={cn('hs-bildirim pointer-events-auto flex items-start gap-2.5 border px-3.5 py-3 shadow-lg', kutu)}
    >
      <Ikon size={20} strokeWidth={1.75} className={cn('mt-px shrink-0', ikon)} />
      <p className="m-0 min-w-0 flex-1 break-words text-[15px] leading-snug">{mesaj}</p>
      <button
        type="button"
        onClick={onKapat}
        aria-label="Kapat"
        className="shrink-0 cursor-pointer border-0 bg-transparent p-0.5 text-current opacity-60 hover:opacity-100"
      >
        <X size={16} strokeWidth={1.75} />
      </button>
    </div>
  );
}

/** PageHeader'ın bildirimlerin gösterileceği boşluğu kaydetmesi için ref callback. */
export function useBildirimAlani() {
  return useToast().alanKaydet;
}

export function useToast() {
  const toast = useContext(ToastContext);
  if (!toast) throw new Error('useToast ToastProvider içinde kullanılmalı');
  return toast;
}

/**
 * useState('') yerine kullanılır: [hata, setHata]. setHata('mesaj') mesajı kırmızı bildirim olarak gösterir
 * (aynı mesaj art arda gelse de) ve state'e yazmaz. setHata('mesaj', {sessiz: true}) bildirim çıkarmaz,
 * mesajı state'te tutar; sayfa yükleme hatalarında satır içi <Hata> kartı (Tekrar dene) için kullanılır.
 */
export function useHata() {
  const toast = useToast();
  const [hata, setHataState] = useState('');
  const setHata = useCallback(
    (mesaj, { sessiz = false } = {}) => {
      if (mesaj && !sessiz) {
        toast.hata(mesaj);
      } else {
        setHataState(mesaj);
      }
    },
    [toast],
  );
  return [hata, setHata];
}
