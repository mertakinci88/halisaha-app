-- 1) Ödeme tablolarına işletme bilgisi: rapor sorguları tüm işletmelerin ödemelerini taramak yerine doğrudan
--    (isletme_id, odeme_tarihi) indeksiyle yalnızca ilgili işletmenin ödemelerini okur. İşletme bilgisi, bağlı olduğu kaydın
--    (rezervasyon / öğrenci / envanter satışı / abonelik) işletmesiyle aynı olmak zorundadır; bileşik yabancı anahtar bunu garanti eder.
-- 2) Öğrenci / veli adı araması için trigram indeksi ("içinde geçen" aramaları da indeks kullanır).

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Bileşik yabancı anahtarın hedefi olabilmesi için (id, isletme_id) tekil olmalıdır
ALTER TABLE rezervasyon    ADD CONSTRAINT uk_rezervasyon_id_isletme    UNIQUE (id, isletme_id);
ALTER TABLE ogrenci        ADD CONSTRAINT uk_ogrenci_id_isletme        UNIQUE (id, isletme_id);
ALTER TABLE envanter_satis ADD CONSTRAINT uk_envanter_satis_id_isletme UNIQUE (id, isletme_id);
ALTER TABLE abonelik       ADD CONSTRAINT uk_abonelik_id_isletme       UNIQUE (id, isletme_id);

-- Rezervasyon ödemeleri
ALTER TABLE rezervasyon_odeme ADD COLUMN isletme_id BIGINT;
UPDATE rezervasyon_odeme o SET isletme_id = r.isletme_id FROM rezervasyon r WHERE r.id = o.rezervasyon_id;
ALTER TABLE rezervasyon_odeme
    ALTER COLUMN isletme_id SET NOT NULL,
    ADD CONSTRAINT fk_rezervasyon_odeme_isletme FOREIGN KEY (rezervasyon_id, isletme_id) REFERENCES rezervasyon (id, isletme_id);
DROP INDEX idx_rezervasyon_odeme_tarih;
CREATE INDEX idx_rezervasyon_odeme_isletme_tarih ON rezervasyon_odeme (isletme_id, odeme_tarihi) INCLUDE (tutar, yontem);

-- Öğrenci aylık ödemeleri
ALTER TABLE ogrenci_odeme ADD COLUMN isletme_id BIGINT;
UPDATE ogrenci_odeme p SET isletme_id = o.isletme_id FROM ogrenci o WHERE o.id = p.ogrenci_id;
ALTER TABLE ogrenci_odeme
    ALTER COLUMN isletme_id SET NOT NULL,
    ADD CONSTRAINT fk_ogrenci_odeme_isletme FOREIGN KEY (ogrenci_id, isletme_id) REFERENCES ogrenci (id, isletme_id) ON DELETE CASCADE;
DROP INDEX idx_ogrenci_odeme_tarih;
DROP INDEX idx_ogrenci_odeme_donem;
CREATE INDEX idx_ogrenci_odeme_isletme_tarih ON ogrenci_odeme (isletme_id, odeme_tarihi) INCLUDE (odenen_tutar, odeme_sekli) WHERE odeme_tarihi IS NOT NULL;
CREATE INDEX idx_ogrenci_odeme_isletme_donem ON ogrenci_odeme (isletme_id, yil, ay) INCLUDE (ogrenci_id, odenen_tutar);

-- Envanter satış ödemeleri
ALTER TABLE envanter_satis_odeme ADD COLUMN isletme_id BIGINT;
UPDATE envanter_satis_odeme p SET isletme_id = s.isletme_id FROM envanter_satis s WHERE s.id = p.satis_id;
ALTER TABLE envanter_satis_odeme
    ALTER COLUMN isletme_id SET NOT NULL,
    ADD CONSTRAINT fk_envanter_satis_odeme_isletme FOREIGN KEY (satis_id, isletme_id) REFERENCES envanter_satis (id, isletme_id) ON DELETE CASCADE;
DROP INDEX idx_envanter_satis_odeme_tarih;
CREATE INDEX idx_envanter_satis_odeme_isletme_tarih ON envanter_satis_odeme (isletme_id, odeme_tarihi) INCLUDE (tutar, odeme_sekli);

-- Abonelik ödemeleri
ALTER TABLE abonelik_odeme ADD COLUMN isletme_id BIGINT;
UPDATE abonelik_odeme p SET isletme_id = a.isletme_id FROM abonelik a WHERE a.id = p.abonelik_id;
ALTER TABLE abonelik_odeme
    ALTER COLUMN isletme_id SET NOT NULL,
    ADD CONSTRAINT fk_abonelik_odeme_isletme FOREIGN KEY (abonelik_id, isletme_id) REFERENCES abonelik (id, isletme_id) ON DELETE CASCADE;
DROP INDEX idx_abonelik_odeme_tarih;
CREATE INDEX idx_abonelik_odeme_isletme_tarih ON abonelik_odeme (isletme_id, odeme_tarihi) INCLUDE (tutar, odeme_sekli);

-- Öğrenci ve veli adı araması (küçük/büyük harf duyarsız, "içinde geçen")
CREATE INDEX idx_ogrenci_ad_trgm ON ogrenci USING gin (lower(ad_soyad) gin_trgm_ops);
CREATE INDEX idx_ogrenci_veli_ad_trgm ON ogrenci USING gin (lower(veli_ad_soyad) gin_trgm_ops) WHERE veli_ad_soyad IS NOT NULL;
