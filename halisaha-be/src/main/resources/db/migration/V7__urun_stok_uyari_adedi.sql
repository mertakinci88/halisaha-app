-- Ürün bazında stok uyarı eşiği: stok bu adetten azsa "Stok az" uyarısı verilir.
-- Mevcut ürünler önceki sabit eşik olan 10 ile başlar.
ALTER TABLE urun ADD COLUMN stok_uyari_adedi INTEGER NOT NULL DEFAULT 10 CHECK (stok_uyari_adedi >= 0);
