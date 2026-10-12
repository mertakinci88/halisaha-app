import { Tag } from '@/components/ui/tag';
import { ODEME_DURUMU_LABEL } from '@/lib/format';

// Ödendi yeşil, Ödeme bekliyor / Kısmi ödendi sarı, Ödenmedi kırmızı
const RENK = {
  ODENDI: 'bg-basari-100 text-basari-800',
  ODEME_BEKLIYOR: 'bg-yellow-100 text-yellow-800',
  KISMI_ODENDI: 'bg-yellow-100 text-yellow-800',
  ODENMEDI: 'bg-red-100 text-red-800',
};

// belirgin: etiketin etrafına aynı renkte çerçeve çizer (yazı kalınlığı değişmez)
const CERCEVE = {
  ODENDI: 'border border-basari-600',
  ODEME_BEKLIYOR: 'border border-yellow-600',
  KISMI_ODENDI: 'border border-yellow-600',
  ODENMEDI: 'border border-hata-600',
};

export default function OdemeDurumuEtiketi({ durum, belirgin = false }) {
  if (!durum) return <span className="text-ink/55">—</span>;
  return <Tag className={`${RENK[durum] || ''} ${belirgin ? CERCEVE[durum] || '' : ''}`}>{ODEME_DURUMU_LABEL[durum] || '—'}</Tag>;
}
