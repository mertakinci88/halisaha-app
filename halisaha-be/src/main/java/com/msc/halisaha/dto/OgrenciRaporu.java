package com.msc.halisaha.dto;

import java.math.BigDecimal;
import java.util.List;

/** Seçilen ay için öğrenci ödeme tahsilat özeti; borçlu listesi öğrenci ekranındaki "ödeme bekleyen" filtresindedir. */
public record OgrenciRaporu(
        int yil,
        int ay,
        long aktifOgrenci,
        long pasifOgrenci,
        long odeyen,
        long odemeyen,
        /** Ödeyen / aktif öğrenci, yüzde (aktif öğrenci yoksa 0). */
        BigDecimal tahsilatOrani,
        /** Seçilen ay ve bir önceki ay hiç ödeme yapmamış aktif öğrenci sayısı. */
        long ardisikOdemeyen,
        long yeniKayit,
        List<GrupSatiri> gruplar,
        List<AySatiri> trend
) {
    public record GrupSatiri(Long grupId, String grupAd, long aktif, long odeyen, BigDecimal oran) {
    }

    /** Son 12 ayın tahsilatı: ödeme yapan öğrenci sayısı ve tahsil edilen tutar. */
    public record AySatiri(int yil, int ay, long odeyen, BigDecimal tutar) {
    }
}
