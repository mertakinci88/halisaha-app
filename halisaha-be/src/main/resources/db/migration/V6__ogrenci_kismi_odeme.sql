-- Bir ayın birden fazla ödemesi olabilir (kısmi ödeme); ay, "tamamı ödendi" işaretli bir ödeme varsa ödenmiş sayılır.
ALTER TABLE ogrenci_odeme DROP CONSTRAINT ogrenci_odeme_ay_tekil;

ALTER TABLE ogrenci_odeme ADD COLUMN tamam boolean NOT NULL DEFAULT true;

-- Mevcut kayıtlar: tutarı olan ay ödenmiş (tamam), yalnızca not olan ay ödenmemiş kalır
UPDATE ogrenci_odeme SET tamam = false WHERE odenen_tutar = 0;

CREATE INDEX idx_ogrenci_odeme_ogrenci_donem ON ogrenci_odeme USING btree (ogrenci_id, yil, ay);
