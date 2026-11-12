-- kullanici_id artık yoklamayı İLK alan kullanıcıdır (önceden son kaydeden üzerine yazılıyordu; mevcut kayıtlar için bilinen en iyi bilgi).
-- Yoklama sonradan değiştirilirse değiştiren guncelleyen_kullanici_id'de tutulur; hiç güncellenmediyse NULL.
ALTER TABLE yoklama ADD COLUMN guncelleyen_kullanici_id bigint;
ALTER TABLE yoklama ADD CONSTRAINT yoklama_guncelleyen_kullanici_id_fkey FOREIGN KEY (guncelleyen_kullanici_id) REFERENCES kullanici(id);
