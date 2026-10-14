-- Çıkış yapmadan süresi dolan oturumlar için yeni giriş geçmişi olayı
ALTER TABLE oturum_log DROP CONSTRAINT oturum_log_olay_check;
ALTER TABLE oturum_log ADD CONSTRAINT oturum_log_olay_check
    CHECK (olay IN ('GIRIS', 'CIKIS', 'BASARISIZ_GIRIS', 'OTURUM_SURESI_DOLDU'));

-- Temizlik işi: süresi dolmuş, iptal edilmemiş oturumları ve eski kayıtları verimli bulmak için
CREATE INDEX idx_refresh_token_expiry ON refresh_token (expiry_date);
CREATE INDEX idx_oturum_log_zaman ON oturum_log (zaman);
