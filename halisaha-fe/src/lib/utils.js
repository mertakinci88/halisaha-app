import { clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs) {
  return twMerge(clsx(inputs));
}

// Arama için sadeleştirme: büyük/küçük harf ve Türkçe karakter (I/ı/İ/i, ş, ğ, ü, ö, ç) ayrımı yok. Backend'deki AramaMetni ile aynı kural.
const ARAMA_KAYNAK = 'İIıŞşĞğÜüÖöÇçÂâÎîÛû';
const ARAMA_HEDEF = 'iiissgguuooccaaiiuu';
export function aramaSade(metin) {
  return Array.from((metin ?? '').trim())
    .map((c) => { const i = ARAMA_KAYNAK.indexOf(c); return i >= 0 ? ARAMA_HEDEF[i] : c; })
    .join('').toLowerCase().replace(/\s+/g, ' ');
}
