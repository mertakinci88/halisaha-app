package com.msc.halisaha.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EnvanterRaporu(
        LocalDate baslangic,
        LocalDate bitis,
        long satisAdedi,
        BigDecimal satisTutari,
        BigDecimal tahsilEdilen,
        BigDecimal bekleyen,
        List<UrunSatiri> urunler,
        List<SetSatiri> setler,
        List<StokSatiri> kritikStok
) {
    public record UrunSatiri(String tur, String beden, long adet, BigDecimal tutar) {
    }

    /** Set adı olmayan (setsiz) satışlar için {@code ad} null gelir. */
    public record SetSatiri(String ad, long adet, BigDecimal tutar) {
    }

    public record StokSatiri(String tur, String beden, int adet, int uyariAdedi) {
    }
}
