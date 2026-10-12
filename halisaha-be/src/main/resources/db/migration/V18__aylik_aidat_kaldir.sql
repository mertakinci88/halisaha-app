-- Aylık aidat mantığı kaldırıldı: her ay ne kadar ödendiyse o girilir (tutar üst sınırı yok).
-- Ödeme kaydı olan ay "Ödendi", kaydı olmayan ay "Ödenmedi" sayılır; tam / kısmi ayrımı yoktur.

ALTER TABLE ogrenci_odeme DROP CONSTRAINT ogrenci_odeme_tutar_sinir;
ALTER TABLE ogrenci_odeme DROP COLUMN aidat_tutari;
ALTER TABLE ogrenci DROP COLUMN aylik_aidat;
