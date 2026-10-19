-- Antrenman: ücretsiz abonelik; yalnızca günü ve saati takvimde kapatır (ödeme, depozito ve ücret yoktur, raporlara girmez).
ALTER TABLE abonelik ADD COLUMN antrenman boolean NOT NULL DEFAULT false;
ALTER TABLE rezervasyon ADD COLUMN antrenman boolean NOT NULL DEFAULT false;

ALTER TABLE abonelik DROP CONSTRAINT abonelik_ucret_check;
ALTER TABLE abonelik DROP CONSTRAINT abonelik_haftalik_ucret_check;
ALTER TABLE abonelik ADD CONSTRAINT abonelik_ucret_check
    CHECK (depozito >= 0 AND haftalik_ucret >= 0 AND (antrenman OR (depozito > 0 AND haftalik_ucret > 0)));
