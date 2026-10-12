-- Aylık öğrenci ödemesine not (ör. "babası getirdi", "ağustos aidatı da bu ödemede")
ALTER TABLE ogrenci_odeme ADD COLUMN "not" VARCHAR(255);
