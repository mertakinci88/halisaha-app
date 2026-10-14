import { createElement } from 'react';
import { ArrowRightLeft, CalendarClock, ClipboardCheck, CupSoda, Landmark, Layers, Package, Square, Users } from 'lucide-react';

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
};

export const ModulIkon = ({ modul, size = 16 }) => createElement(MODUL_IKON[modul], { size, strokeWidth: 1.5 });
