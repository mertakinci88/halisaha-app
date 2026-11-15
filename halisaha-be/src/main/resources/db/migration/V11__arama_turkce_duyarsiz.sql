-- Ad/soyad aramasında Türkçe karakter ve büyük/küçük harf duyarsızlığı:
-- I, ı, İ, i hepsi 'i'; ş/ğ/ü/ö/ç ve şapkalı harfler ASCII karşılığına indirgenir. Uygulama tarafı da aynı kuralla indirger (AramaMetni).
CREATE OR REPLACE FUNCTION tr_sade(metin text) RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE
    AS $$ SELECT lower(translate(metin, 'İIıŞşĞğÜüÖöÇçÂâÎîÛû', 'iiissgguuooccaaiiuu')) $$;

DROP INDEX IF EXISTS idx_ogrenci_ad_trgm;
DROP INDEX IF EXISTS idx_ogrenci_veli_ad_trgm;
CREATE INDEX idx_ogrenci_ad_trgm ON ogrenci USING gin (tr_sade(ad_soyad) gin_trgm_ops);
CREATE INDEX idx_ogrenci_veli_ad_trgm ON ogrenci USING gin (tr_sade(veli_ad_soyad) gin_trgm_ops) WHERE (veli_ad_soyad IS NOT NULL);
