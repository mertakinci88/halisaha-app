-- Öğrenci detayındaki "Yoklama bilgileri" kartı için personel işlem yetkisi
ALTER TABLE kullanici_yetki DROP CONSTRAINT kullanici_yetki_modul_check;
ALTER TABLE kullanici_yetki ADD CONSTRAINT kullanici_yetki_modul_check
    CHECK (modul IN ('OGRENCI', 'GRUP', 'SAHA', 'URUN', 'YOKLAMA', 'ENVANTER', 'HESAP', 'TRANSFER', 'ABONELIK',
                     'OGRENCI_TRANSFER', 'OGRENCI_ODEME', 'OGRENCI_ENVANTER', 'OGRENCI_YOKLAMA'));
