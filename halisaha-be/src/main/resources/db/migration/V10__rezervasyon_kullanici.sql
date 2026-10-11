-- Rezervasyonu sisteme giren kullanıcı. Bu alan eklenmeden önceki kayıtlarda bilinmediği için NULL kalır.
ALTER TABLE rezervasyon ADD COLUMN kullanici_id BIGINT REFERENCES kullanici (id);

CREATE INDEX idx_rezervasyon_kullanici ON rezervasyon (kullanici_id);
