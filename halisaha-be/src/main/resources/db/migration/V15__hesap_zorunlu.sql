-- Havale / EFT ile alınan her ödeme ve öğrenci ödeme şekli bir hesaba bağlı olmalıdır; diğer yöntemlerde hesap yoktur.
-- Eski kayıtlarda hesap bilgisi tutulmadığı için, hesapsız Havale / EFT kaydı olan her işletmeye
-- "Eski kayıtlar" adında pasif bir yer tutucu hesap açılır ve bu kayıtlar ona bağlanır
-- (gerçek bir hesaba para atfedilmez; raporlarda ayrı bir kalem olarak görünür).

INSERT INTO hesap (isletme_id, ad, iban, hesap_sahibi, durum, kayit_tarih)
SELECT i.id, 'Eski kayıtlar (hesap belirtilmemiş)', 'TR000000000000000000000000', '-', 'PASIF', now()
FROM isletme i
WHERE EXISTS (SELECT 1 FROM odeme o JOIN rezervasyon r ON r.id = o.rezervasyon_id
              WHERE r.isletme_id = i.id AND o.yontem = 'HAVALE_EFT' AND o.hesap_id IS NULL)
   OR EXISTS (SELECT 1 FROM ogrenci g
              WHERE g.isletme_id = i.id AND g.odeme_sekli = 'HAVALE_EFT' AND g.hesap_id IS NULL);

UPDATE odeme o
SET hesap_id = (SELECT h.id FROM hesap h
                WHERE h.isletme_id = r.isletme_id AND h.ad = 'Eski kayıtlar (hesap belirtilmemiş)')
FROM rezervasyon r
WHERE r.id = o.rezervasyon_id AND o.yontem = 'HAVALE_EFT' AND o.hesap_id IS NULL;

UPDATE ogrenci g
SET hesap_id = (SELECT h.id FROM hesap h
                WHERE h.isletme_id = g.isletme_id AND h.ad = 'Eski kayıtlar (hesap belirtilmemiş)')
WHERE g.odeme_sekli = 'HAVALE_EFT' AND g.hesap_id IS NULL;

-- Nakit / Kart kayıtlarında hesap tutulmaz
UPDATE odeme   SET hesap_id = NULL WHERE yontem <> 'HAVALE_EFT' AND hesap_id IS NOT NULL;
UPDATE ogrenci SET hesap_id = NULL WHERE COALESCE(odeme_sekli, '') <> 'HAVALE_EFT' AND hesap_id IS NOT NULL;

ALTER TABLE odeme ADD CONSTRAINT odeme_hesap_tutarli
    CHECK ((yontem = 'HAVALE_EFT') = (hesap_id IS NOT NULL));
ALTER TABLE ogrenci ADD CONSTRAINT ogrenci_hesap_tutarli
    CHECK ((COALESCE(odeme_sekli, '') = 'HAVALE_EFT') = (hesap_id IS NOT NULL));
