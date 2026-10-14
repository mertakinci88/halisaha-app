-- Veri büyüdüğünde sorguların yavaşlamaması için eksik indeksler (rapor sorguları ve yabancı anahtar kontrolleri).
-- Yalnızca dolu (NULL olmayan) değerler için "kısmi" indeks kullanılan yerlerde indeks küçük kalır.

-- Rapor: öğrenci aylık ödemeleri ödeme tarihine göre (Finans)
CREATE INDEX idx_ogrenci_odeme_tarih ON ogrenci_odeme (odeme_tarihi) WHERE odeme_tarihi IS NOT NULL;

-- Rapor: envanter satışları işletme + kayıt tarihine göre; öğrencinin envanter satışları (liste filtresi, öğrenci silme kontrolü)
CREATE INDEX idx_envanter_satis_isletme_kayit ON envanter_satis (isletme_id, kayit_tarihi);
CREATE INDEX idx_envanter_satis_ogrenci_id ON envanter_satis (ogrenci_id);
-- Set silinirken satışların set bağlantısını kaldıran güncelleme
CREATE INDEX idx_envanter_satis_set ON envanter_satis (set_id) WHERE set_id IS NOT NULL;
-- Ödemesi alınmış, teslim edilmemiş envanterler (Panel, öğrenci listesi "eksik envanter" filtresi)
CREATE INDEX idx_envanter_satis_kalem_teslim_bekleyen ON envanter_satis_kalem (satis_id) WHERE odendi AND NOT teslim_edildi;
CREATE INDEX idx_envanter_stok_hareket_satis ON envanter_stok_hareket (satis_id) WHERE satis_id IS NOT NULL;

-- Grup silme kontrolü ve grup bazlı öğrenci sorguları
CREATE INDEX idx_ogrenci_grup ON ogrenci (grup_id) WHERE grup_id IS NOT NULL;

-- Ürün silme kontrolü
CREATE INDEX idx_rezervasyon_kalem_urun ON rezervasyon_kalem (urun_id) WHERE urun_id IS NOT NULL;

-- Giriş yenileme: kullanıcının geçerli (iptal edilmemiş) oturumları
CREATE INDEX idx_refresh_token_kullanici ON refresh_token (kullanici_id) WHERE NOT revoked;

-- Abonelik: saha silme kontrolü
CREATE INDEX idx_abonelik_saha ON abonelik (saha_id);

-- Hesap silme kontrolü (hesabın ödeme / iadede kullanılıp kullanılmadığı)
CREATE INDEX idx_rezervasyon_odeme_hesap ON rezervasyon_odeme (hesap_id) WHERE hesap_id IS NOT NULL;
CREATE INDEX idx_ogrenci_odeme_hesap ON ogrenci_odeme (hesap_id) WHERE hesap_id IS NOT NULL;
CREATE INDEX idx_envanter_satis_odeme_hesap ON envanter_satis_odeme (hesap_id) WHERE hesap_id IS NOT NULL;
CREATE INDEX idx_abonelik_odeme_hesap ON abonelik_odeme (hesap_id) WHERE hesap_id IS NOT NULL;
CREATE INDEX idx_iade_hesap ON iade (hesap_id) WHERE hesap_id IS NOT NULL;

-- Kafeterya (ürün) kalemleri az olduğundan yalnızca onlar için küçük indeks: Finans'taki kafeterya satışı sorgusu
CREATE INDEX idx_rezervasyon_kalem_urun_tip ON rezervasyon_kalem (rezervasyon_id) WHERE tip = 'URUN';
