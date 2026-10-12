-- Aylık kayıt yalnızca not içerebilir (tutar girilmeden): o ayın ödemesi yoktur, sadece açıklama vardır.
-- Ödeme şekli artık varsayılan değil kullanıcı seçimidir; tutar girilmeyen kayıtta şekil / tarih boş olabilir.
-- Ay "ödendi" sayılır ancak odenen_tutar > 0 ise.

ALTER TABLE ogrenci_odeme DROP CONSTRAINT ogrenci_odeme_odenen_tutar_check;
ALTER TABLE ogrenci_odeme ADD CONSTRAINT ogrenci_odeme_odenen_tutar_check CHECK (odenen_tutar >= 0);
ALTER TABLE ogrenci_odeme ALTER COLUMN odeme_sekli DROP NOT NULL;
ALTER TABLE ogrenci_odeme ALTER COLUMN odeme_tarihi DROP NOT NULL;
ALTER TABLE ogrenci_odeme ADD CONSTRAINT ogrenci_odeme_dolu CHECK (odenen_tutar > 0 OR "not" IS NOT NULL);
ALTER TABLE ogrenci_odeme ADD CONSTRAINT ogrenci_odeme_odeme_tutarli
    CHECK (odenen_tutar = 0 OR (odeme_sekli IS NOT NULL AND odeme_tarihi IS NOT NULL));
