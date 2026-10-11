import { useEffect, useState } from 'react';

/** CSS medya sorgusunun (ör. '(max-width: 767px)') anlık sonucu; yalnızca aktif görünümün render edilmesi için. */
export function useMedia(sorgu) {
  const [eslesen, setEslesen] = useState(() => (typeof window !== 'undefined' ? window.matchMedia(sorgu).matches : false));
  useEffect(() => {
    const m = window.matchMedia(sorgu);
    const dinle = (e) => setEslesen(e.matches);
    setEslesen(m.matches);
    m.addEventListener('change', dinle);
    return () => m.removeEventListener('change', dinle);
  }, [sorgu]);
  return eslesen;
}
