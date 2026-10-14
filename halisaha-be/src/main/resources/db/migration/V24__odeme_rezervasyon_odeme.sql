-- "odeme" tablosu yalnızca rezervasyon ödemelerini tutar; öğrenci ve envanter ödemelerinden ayırt edilsin diye adı netleştirildi.
ALTER TABLE odeme RENAME TO rezervasyon_odeme;
ALTER INDEX idx_odeme_rezervasyon RENAME TO idx_rezervasyon_odeme_rezervasyon;
ALTER INDEX idx_odeme_tarih RENAME TO idx_rezervasyon_odeme_tarih;
ALTER TABLE rezervasyon_odeme RENAME CONSTRAINT odeme_pkey TO rezervasyon_odeme_pkey;
