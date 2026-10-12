import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Field, Select } from '@/components/ui/input';
import { hesapService } from '@/services';
import { useAuth } from '@/context/AuthContext';

export const HESAP_SECIM_UYARISI = 'Lütfen Havale / EFT hesabını seçiniz';
export const HESAP_TANIMSIZ_UYARISI = 'Havale / EFT için önce Hesaplar menüsünden hesap tanımlanmalıdır';

/**
 * "Havale / EFT hesabı" combosu: HESAPLAR menüsünde tanımlı aktif hesapları listeler.
 * Kayıtta zaten seçili olan hesap pasife alınmış olsa da listede kalır. Değer hesap id'si ('' = seçilmedi).
 * Havale / EFT'de hesap seçimi zorunludur (form gönderiminde tarayıcı uyarısı). Hiç aktif hesap tanımlı
 * değilse de kayıt yapılamaz; combo yerine hesap tanımlama yönlendirmesi gösterilir. Form olmayan ekranlar
 * için `onSecenekler(sayi)` seçilebilir hesap sayısını bildirir.
 */
export default function HesapSecimi({ value, onChange, onSecenekler, label = 'Havale / EFT hesabı' }) {
  const { yetkiVar } = useAuth();
  const [hesaplar, setHesaplar] = useState(null);

  useEffect(() => {
    hesapService
      .list()
      .then(setHesaplar)
      .catch(() => setHesaplar([]));
  }, []);

  const secilebilir = (hesaplar || []).filter((h) => h.durum === 'AKTIF' || String(h.id) === String(value));
  const sayi = hesaplar === null ? null : secilebilir.length;

  useEffect(() => {
    if (sayi !== null) onSecenekler?.(sayi);
  }, [sayi]);

  if (hesaplar === null) return null;

  if (secilebilir.length === 0) {
    // Hesap tanımlı değilse Havale / EFT kaydedilemez: zorunlu boş combo formu uyarıyla durdurur
    return (
      <Field label={label}>
        <Select value="" onChange={() => {}} onInvalid={(e) => e.target.setCustomValidity(HESAP_TANIMSIZ_UYARISI)} required>
          <option value="">Tanımlı hesap yok</option>
        </Select>
        <p className="m-0 mt-1.5 text-[13px] text-ink/55">
          {yetkiVar('HESAP') ? (
            <>
              Havale / EFT için önce{' '}
              <Link to="/hesaplar/yeni" className="text-accent">
                hesap tanımlayın
              </Link>
              .
            </>
          ) : (
            'Havale / EFT için hesap tanımı gerekir; yöneticinize başvurun.'
          )}
        </p>
      </Field>
    );
  }

  return (
    <Field label={label}>
      <Select
        value={value ?? ''}
        onChange={(e) => {
          e.target.setCustomValidity('');
          onChange(e.target.value);
        }}
        onInvalid={(e) => e.target.setCustomValidity(HESAP_SECIM_UYARISI)}
        required
      >
        <option value="">Hesap seçin</option>
        {secilebilir.map((h) => (
          <option key={h.id} value={h.id}>
            {h.ad}
            {h.durum === 'PASIF' ? ' (pasif)' : ''}
          </option>
        ))}
      </Select>
    </Field>
  );
}
