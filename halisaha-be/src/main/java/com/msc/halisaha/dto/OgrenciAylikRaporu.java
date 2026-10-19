package com.msc.halisaha.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Birden fazla aya yayılan dönemin öğrenci ödeme özeti; her ay ödemenin ait olduğu aya göre sayılır.
 * Bir ayın ödeme oranı: o ayın sonunda kayıtlı olan ve şu an aktif öğrencilerden ödeme yapanlerin oranı.
 */
public record OgrenciAylikRaporu(
        LocalDate baslangic,
        LocalDate bitis,
        long aktifOgrenci,
        long yeniKayit,
        /** Dönemdeki tüm ödemelerin toplamı. */
        BigDecimal toplamTutar,
        /** Ayların tahsilat oranlarının ortalaması. */
        BigDecimal ortalamaOran,
        /** Toplam tutar / ay sayısı. */
        BigDecimal aylikOrtalamaTutar,
        List<AySatiri> aylar,
        List<GrupSatiri> gruplar
) {
    /** odemeyen: ayını tamamlamayanlar (kısmi dahil); kismi: bunlardan ödeme almış ama ayı tamamlamamış olanlar. */
    public record AySatiri(int yil, int ay, long ogrenci, long odeyen, long odemeyen, long kismi, BigDecimal oran, BigDecimal tutar) {
    }

    public record GrupSatiri(Long grupId, String grupAd, long aktif, BigDecimal toplamTutar, BigDecimal ortalamaOran) {
    }
}
