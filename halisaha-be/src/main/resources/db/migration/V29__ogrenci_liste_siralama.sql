-- Öğrenci listesi ada göre sıralı ve sayfalı gelir (işletme + ad sırası): ilk sayfa için tüm öğrenciler sıralanmaz.
CREATE INDEX idx_ogrenci_isletme_ad ON ogrenci (isletme_id, ad_soyad);
-- Yeni indeksler (isletme + tarih / dönem) bunun yerine geçtiği için eski, tek kolonlu işletme indeksi ogrenci için gereksiz kalır
DROP INDEX idx_ogrenci_isletme;
