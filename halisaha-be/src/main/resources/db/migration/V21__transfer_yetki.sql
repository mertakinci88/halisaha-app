-- Yeni menü yetkisi: Transferler ekranı (talepleri onaylama / reddetme) personele ayrıca verilebilir
ALTER TABLE kullanici_yetki DROP CONSTRAINT IF EXISTS kullanici_yetki_modul_check;
ALTER TABLE kullanici_yetki ADD CONSTRAINT kullanici_yetki_modul_check
    CHECK (modul IN ('OGRENCI', 'GRUP', 'SAHA', 'URUN', 'YOKLAMA', 'ENVANTER', 'HESAP', 'TRANSFER'));
