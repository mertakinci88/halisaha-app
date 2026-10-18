-- Set artık bir bedene bağlı ve kendi tutarı olan bir paket: kalemlerin tek tek fiyatı yoktur.
ALTER TABLE envanter_set ADD COLUMN beden character varying(10);
ALTER TABLE envanter_set ADD COLUMN tutar numeric(10,2);

-- Varsa eski setler: beden = bedenli kalemlerinin bedeni, tutar = kalem fiyatları toplamı
UPDATE envanter_set s SET
    beden = COALESCE((SELECT MIN(e.beden) FROM envanter_set_kalem k JOIN envanter e ON e.id = k.envanter_id
                      WHERE k.set_id = s.id AND e.beden <> 'STANDART'), 'STANDART'),
    tutar = GREATEST(COALESCE((SELECT SUM(e.satis_fiyati) FROM envanter_set_kalem k JOIN envanter e ON e.id = k.envanter_id
                               WHERE k.set_id = s.id), 0), 1);

ALTER TABLE envanter_set ALTER COLUMN beden SET NOT NULL;
ALTER TABLE envanter_set ALTER COLUMN tutar SET NOT NULL;
ALTER TABLE envanter_set ADD CONSTRAINT envanter_set_tutar_check CHECK (tutar > 0);

-- Set satışında tutar sette belirlenir; kalemlerin birim fiyatı yoktur (0). Eski satışlarda false kalır.
ALTER TABLE envanter_satis ADD COLUMN sabit_tutar boolean DEFAULT false NOT NULL;
