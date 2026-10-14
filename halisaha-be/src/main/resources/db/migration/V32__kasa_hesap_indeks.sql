-- Kasa raporu havale ödemelerini hesaba (banka) göre de gösterir: hesap kolonu da kapsayan indeksle tablo okunmaz
DROP INDEX idx_rezervasyon_odeme_isletme_tarih;
CREATE INDEX idx_rezervasyon_odeme_isletme_tarih ON rezervasyon_odeme (isletme_id, odeme_tarihi) INCLUDE (tutar, yontem, kullanici_id, hesap_id);
DROP INDEX idx_ogrenci_odeme_isletme_tarih;
CREATE INDEX idx_ogrenci_odeme_isletme_tarih ON ogrenci_odeme (isletme_id, odeme_tarihi) INCLUDE (odenen_tutar, odeme_sekli, kullanici_id, hesap_id) WHERE odeme_tarihi IS NOT NULL;
DROP INDEX idx_envanter_satis_odeme_isletme_tarih;
CREATE INDEX idx_envanter_satis_odeme_isletme_tarih ON envanter_satis_odeme (isletme_id, odeme_tarihi) INCLUDE (tutar, odeme_sekli, kullanici_id, hesap_id);
DROP INDEX idx_abonelik_odeme_isletme_tarih;
CREATE INDEX idx_abonelik_odeme_isletme_tarih ON abonelik_odeme (isletme_id, odeme_tarihi) INCLUDE (tutar, odeme_sekli, kullanici_id, hesap_id);
