-- Abonelik saati artık dakika olarak tutulur: buçuklu saatlere (ör. 20:30) izin verir.
-- Başlangıç: işletme gününün 00:00'ından itibaren dakika (12:00 = 720, 02:00 ertesi gün = 1560); sadece tam saat ve buçuklu (30'un katı).
ALTER TABLE abonelik ADD COLUMN baslangic_dakika INTEGER, ADD COLUMN sure_dakika INTEGER;
UPDATE abonelik SET baslangic_dakika = baslangic_saat * 60, sure_dakika = sure_saat * 60;
ALTER TABLE abonelik
    ALTER COLUMN baslangic_dakika SET NOT NULL,
    ALTER COLUMN sure_dakika SET NOT NULL,
    ADD CONSTRAINT abonelik_baslangic_dakika_aralik CHECK (baslangic_dakika BETWEEN 720 AND 1530 AND baslangic_dakika % 30 = 0),
    ADD CONSTRAINT abonelik_sure_dakika_aralik CHECK (sure_dakika >= 30 AND sure_dakika % 30 = 0);
ALTER TABLE abonelik DROP COLUMN baslangic_saat, DROP COLUMN sure_saat;
-- Kapanış 02:00 (ertesi gün) = 1560. baslangic_saat + sure_saat kısıtı sütunlarla birlikte düştü, dakika karşılığı eklenir
ALTER TABLE abonelik ADD CONSTRAINT abonelik_saat_sinir CHECK (baslangic_dakika + sure_dakika <= 1560);
