import { useCallback, useEffect, useState } from 'react';
import { envanterTurService } from '@/services';

export const STANDART = 'STANDART';

/** Listelerde miktar: çift olarak sayılan türlerde (ör. tozluk) "40 çift", diğerlerinde yalnızca sayı (sütun başlığı Adet). */
export const miktar = (birim, n) => (birim === 'çift' ? n + ' çift' : n);

/**
 * İşletmenin envanter türleri (Türkçe alfabetik): [{ kod, id, ad, bedenli, birim, bedenler, kullanimda }].
 * kod = id; ilk yüklemede null. yenile() türler eklendikten / değiştikten sonra listeyi tazeler.
 */
export function useEnvanterTurleri() {
  const [turler, setTurler] = useState(null);
  const yenile = useCallback(
    () =>
      envanterTurService
        .list()
        .then((l) => {
          const liste = l.map((t) => ({ ...t, kod: t.id }));
          setTurler(liste);
          return liste;
        })
        .catch(() => {
          setTurler((onceki) => onceki || []);
          return [];
        }),
    [],
  );
  useEffect(() => {
    yenile();
  }, [yenile]);
  return { turler, yenile };
}

/** Bedenli türlerin tüm bedenleri, ilk görüldükleri sırayla (set bedeni seçimi için). */
export const tumBedenler = (turler) => [...new Set((turler || []).filter((t) => t.bedenli).flatMap((t) => t.bedenler))];
