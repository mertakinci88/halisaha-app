-- Bazı öğrencilerden hiç ödeme alınmaz: kapalıysa öğrenci ödeme durumu, borçlu listesi ve ödeme raporlarının dışında kalır (mevcut kayıtlar etkilenmez).
ALTER TABLE ogrenci ADD COLUMN odeme_alinir boolean NOT NULL DEFAULT true;
