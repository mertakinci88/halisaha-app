-- Rezervasyonun sisteme girildiği an. Mevcut kayıtlar için bu bilgi tutulmadığından NULL kalır.
ALTER TABLE rezervasyon ADD COLUMN kayit_tarih TIMESTAMP(6);
