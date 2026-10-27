package com.msc.halisaha.dto;

import java.math.BigDecimal;
import java.util.List;

/** Seçilen ay için öğrenci ödeme tahsilat özeti; borçlu listesi öğrenci ekranındaki "ödeme bekleyen" filtresindedir. */
public record OgrenciRaporu(
        int yil,
        int ay,
        /** Ödemesi alınan aktif öğrenci sayısı (ödemesizler hariç). */
        long aktifOgrenci,
        long pasifOgrenci,
        long odeyen,
        /** Ayını tamamlamamış aktif öğrenciler (kısmi ödeme yapanlar dahil). */
        long odemeyen,
        /** Odemeyen içinden: ödeme almış ama ayı tamamlamamış olanlar; hiç ödeme yapmayan = odemeyen - kismiOdeyen. */
        long kismiOdeyen,
        /** Ödeyen / aktif öğrenci, yüzde (aktif öğrenci yoksa 0). */
        BigDecimal tahsilatOrani,
        /** Seçilen ay ve bir önceki ay hiç ödeme yapmamış aktif öğrenci sayısı. */
        long ardisikOdemeyen,
        long yeniKayit,
        /** Ödeme alınmayan aktif öğrenci sayısı; aktifOgrenci ve tahsilat hesaplarına dahil değildir. */
        long odemesizOgrenci,
        List<GrupSatiri> gruplar,
        List<AySatiri> trend
) {
    /** tutar: o ay grubun öğrencilerinden alınan toplam ödeme. */
    public record GrupSatiri(Long grupId, String grupAd, long aktif, long odeyen, BigDecimal oran, BigDecimal tutar) {
    }

    /** Son 12 ayın tahsilatı: ödeme yapan öğrenci sayısı ve tahsil edilen tutar. */
    public record AySatiri(int yil, int ay, long odeyen, BigDecimal tutar) {
    }
}
