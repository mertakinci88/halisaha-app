package com.msc.halisaha.dto;

import com.msc.halisaha.entity.Ogrenci;
import com.msc.halisaha.entity.Yoklama;
import com.msc.halisaha.entity.YoklamaDurum;

import java.time.LocalDateTime;

/**
 * Bir öğrencinin seçili günkü yoklaması; durum null ise öğrenci o gün için işaretlenmemiştir.
 * guncelleyen/guncellemeTarihi yalnızca kayıt ilk alındıktan sonra değiştirildiyse dolu gelir.
 */
public record YoklamaSatir(
        Long ogrenciId,
        String adSoyad,
        YoklamaDurum durum,
        String not,
        String olusturan,
        LocalDateTime olusturmaTarihi,
        String guncelleyen,
        LocalDateTime guncellemeTarihi
) {
    public static YoklamaSatir of(Ogrenci o, Yoklama y) {
        if (y == null) {
            return new YoklamaSatir(o.getId(), o.getAdSoyad(), null, null, null, null, null, null);
        }
        boolean guncellendi = y.getGuncelleyenKullanici() != null;
        return new YoklamaSatir(o.getId(), o.getAdSoyad(), y.getDurum(), y.getAciklama(),
                y.getOlusturanKullanici() != null ? y.getOlusturanKullanici().getAdSoyad() : null,
                y.getOlusturmaTarihi(),
                guncellendi ? y.getGuncelleyenKullanici().getAdSoyad() : null,
                guncellendi ? y.getGuncellemeTarihi() : null);
    }
}
