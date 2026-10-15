-- Abonelik depozito modeline geçer: abone kayıtta depozito bırakır, her hafta maç ücretini öder;
-- iptalde kalan depozito ile kalan haftalar oynanır (artan tutar isteğe bağlı iade edilir).

-- Eski "toplam ücret" artık depozito tutarıdır
ALTER TABLE abonelik RENAME COLUMN ucret TO depozito;

-- Maç başına (haftalık) ücret; her haftalık rezervasyon bu tutarla açılır
ALTER TABLE abonelik ADD COLUMN haftalik_ucret NUMERIC(10, 2);
UPDATE abonelik SET haftalik_ucret = GREATEST(ROUND(depozito / 52, 2), 0.01);
ALTER TABLE abonelik ALTER COLUMN haftalik_ucret SET NOT NULL;
ALTER TABLE abonelik ADD CONSTRAINT abonelik_haftalik_ucret_check CHECK (haftalik_ucret > 0);
