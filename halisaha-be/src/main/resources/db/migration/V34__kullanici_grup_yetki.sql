-- Personelin görebileceği gruplar. grup_kisitli = false ise tüm gruplar (varsayılan); true ise yalnızca kullanici_grup'taki gruplar.
ALTER TABLE kullanici ADD COLUMN grup_kisitli BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE kullanici_grup (
    kullanici_id BIGINT NOT NULL REFERENCES kullanici (id) ON DELETE CASCADE,
    grup_id      BIGINT NOT NULL REFERENCES grup (id) ON DELETE CASCADE,
    PRIMARY KEY (kullanici_id, grup_id)
);
