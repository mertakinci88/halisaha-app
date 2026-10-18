import { createElement } from 'react';
import { ArrowRightLeft, CalendarClock, ClipboardCheck, CupSoda, Landmark, Layers, Package, Square, Users, Wallet } from 'lucide-react';

export const TUM_MODULLER = ['OGRENCI', 'GRUP', 'YOKLAMA', 'SAHA', 'URUN', 'ENVANTER', 'HESAP', 'TRANSFER', 'ABONELIK'];

export const MODUL_LABEL = {
  OGRENCI: 'Öğrenciler',
  GRUP: 'Gruplar',
  YOKLAMA: 'Yoklama',
  SAHA: 'Sahalar',
  URUN: 'Kafeterya',
  ENVANTER: 'Envanter',
  HESAP: 'Hesaplar',
  TRANSFER: 'Transferler',
  ABONELIK: 'Aboneler',
  OGRENCI_TRANSFER: 'Transfer bilgileri',
  OGRENCI_ODEME: 'Ödeme bilgileri',
  OGRENCI_ENVANTER: 'Envanter bilgileri',
  OGRENCI_YOKLAMA: 'Yoklama bilgileri',
};

/** Menü ikonları (kenar çubuğundakilerle aynı) */
export const MODUL_IKON = {
  OGRENCI: Users,
  GRUP: Layers,
  YOKLAMA: ClipboardCheck,
  SAHA: Square,
  URUN: CupSoda,
  ENVANTER: Package,
  HESAP: Landmark,
  TRANSFER: ArrowRightLeft,
  ABONELIK: CalendarClock,
  OGRENCI_TRANSFER: ArrowRightLeft,
  OGRENCI_ODEME: Wallet,
  OGRENCI_ENVANTER: Package,
  OGRENCI_YOKLAMA: ClipboardCheck,
};

export const ModulIkon = ({ modul, size = 16 }) => createElement(MODUL_IKON[modul], { size, strokeWidth: 1.5 });

/** Grup yetkisi yalnızca Öğrenciler ya da Yoklama menüsü açıksa gösterilir; aksi halde tüm gruplar (kısıt yok) */
export const GRUP_BAGLI_MODULLER = ['OGRENCI', 'YOKLAMA'];
export const grupYetkisiGecerli = (yetkiler) => GRUP_BAGLI_MODULLER.some((m) => yetkiler.includes(m));

/** Öğrenci detayındaki kartlar için işlem yetkileri; yalnızca Öğrenciler menüsü açıksa verilebilir */
export const OGRENCI_ISLEM_YETKILERI = ['OGRENCI_TRANSFER', 'OGRENCI_ODEME', 'OGRENCI_ENVANTER', 'OGRENCI_YOKLAMA'];
