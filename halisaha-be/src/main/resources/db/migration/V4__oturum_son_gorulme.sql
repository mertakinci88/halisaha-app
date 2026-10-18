-- Tarayıcı kapatılarak bırakılan oturumları anlamak için: istemci düzenli olarak "buradayım" der, son görülme saklanır.
-- Uzun süre görülmeyen oturum için bir kez "tarayıcı kapatıldı" kaydı yazılır; kullanıcı dönerse "oturuma döndü" yazılır.
ALTER TABLE refresh_token ADD COLUMN son_gorulme timestamp(6) with time zone;
ALTER TABLE refresh_token ADD COLUMN kapanis_yazildi boolean DEFAULT false NOT NULL;

CREATE INDEX idx_refresh_token_son_gorulme ON refresh_token USING btree (son_gorulme) WHERE ((NOT revoked) AND (NOT kapanis_yazildi));

ALTER TABLE oturum_log DROP CONSTRAINT oturum_log_olay_check;
ALTER TABLE oturum_log ADD CONSTRAINT oturum_log_olay_check
    CHECK (olay IN ('GIRIS', 'CIKIS', 'BASARISIZ_GIRIS', 'OTURUM_SURESI_DOLDU', 'TARAYICI_KAPATILDI', 'OTURUMA_DONDU'));
