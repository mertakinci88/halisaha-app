-- Öğrenci detayındaki kartlar için ayrı işlem yetkileri
ALTER TABLE kullanici_yetki DROP CONSTRAINT IF EXISTS kullanici_yetki_modul_check;
ALTER TABLE kullanici_yetki ADD CONSTRAINT kullanici_yetki_modul_check
    CHECK (modul IN ('OGRENCI', 'GRUP', 'SAHA', 'URUN', 'YOKLAMA', 'ENVANTER', 'HESAP', 'TRANSFER', 'ABONELIK',
                     'OGRENCI_TRANSFER', 'OGRENCI_ODEME', 'OGRENCI_ENVANTER'));

-- Mevcut personelin erişimi değişmesin: Öğrenciler menüsü olanlar transfer ve ödeme kartlarını, Envanter menüsü olanlar envanter kartını görmeye devam eder
INSERT INTO kullanici_yetki (kullanici_id, modul)
SELECT kullanici_id, 'OGRENCI_TRANSFER' FROM kullanici_yetki WHERE modul = 'OGRENCI'
UNION
SELECT kullanici_id, 'OGRENCI_ODEME' FROM kullanici_yetki WHERE modul = 'OGRENCI'
UNION
SELECT kullanici_id, 'OGRENCI_ENVANTER' FROM kullanici_yetki WHERE modul = 'ENVANTER';
