CREATE TABLE kullanici_yetki (
    kullanici_id BIGINT      NOT NULL REFERENCES kullanici (id),
    modul        VARCHAR(30) NOT NULL CHECK (modul IN ('OGRENCI', 'GRUP', 'SAHA', 'URUN')),
    PRIMARY KEY (kullanici_id, modul)
);
