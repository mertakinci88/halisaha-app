-- Rezervasyon telefonu 11 hane: 05XXXXXXXXX.
-- Eski kayıtlarda boşluk/tire gibi karakterler varsa önce temizlenir; böylece kolon daraltma migration'ı hata vermez.
UPDATE rezervasyon SET tel_no = regexp_replace(tel_no, '\D', '', 'g') WHERE tel_no ~ '\D';

ALTER TABLE rezervasyon ALTER COLUMN tel_no TYPE VARCHAR(11) USING left(tel_no, 11);
