-- Envanter satışında ödemenin hangi yöntemle alındığı (Nakit / Kart / Havale-EFT) tutulur.
-- Havale / EFT'de paranın yattığı hesap da kaydedilir. Bu alanlardan önce yapılmış satışlarda boş kalır (belirtilmemiş).

ALTER TABLE envanter_satis
    ADD COLUMN odeme_sekli VARCHAR(20) CHECK (odeme_sekli IN ('NAKIT', 'KART', 'HAVALE_EFT')),
    ADD COLUMN hesap_id    BIGINT REFERENCES hesap (id);

ALTER TABLE envanter_satis
    ADD CONSTRAINT envanter_satis_hesap_tutarli CHECK ((odeme_sekli = 'HAVALE_EFT') = (hesap_id IS NOT NULL));
