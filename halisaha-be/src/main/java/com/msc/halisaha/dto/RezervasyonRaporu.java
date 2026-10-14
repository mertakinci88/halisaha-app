package com.msc.halisaha.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Doluluk = rezervasyonlu saat / (gün sayısı x günlük 14 çalışma saati). İptaller doluluğa katılmaz. */
public record RezervasyonRaporu(
        LocalDate baslangic,
        LocalDate bitis,
        long rezervasyon,
        long iptal,
        BigDecimal iptalOrani,
        BigDecimal toplamSaat,
        BigDecimal dolulukOrani,
        List<SahaSatiri> sahalar,
        /** Başlangıç saatine göre dağılım; 12-25 (24 = 00:00, 25 = 01:00). */
        List<SaatSatiri> saatler,
        /** Haftanın günü: 1 = Pazartesi … 7 = Pazar. */
        List<GunSatiri> gunler
) {
    public record SahaSatiri(Long sahaId, String ad, long rezervasyon, long iptal, BigDecimal saat, BigDecimal sahaGeliri, BigDecimal dolulukOrani) {
    }

    public record SaatSatiri(int saat, long adet) {
    }

    public record GunSatiri(int gun, long adet) {
    }
}
