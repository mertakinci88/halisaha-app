package com.msc.halisaha.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Devam oranı = Geldi / (Geldi + Gelmedi); izinli kayıtlar orana katılmaz. */
public record YoklamaRaporu(
        LocalDate baslangic,
        LocalDate bitis,
        long yoklamaGunu,
        long geldi,
        long gelmedi,
        long izinli,
        BigDecimal devamOrani,
        List<GrupSatiri> gruplar,
        /** Her grupta dönemde kaç gün yoklama alındığı ve son yoklama (yoklaması hiç alınmayan gruplar da dahil). */
        List<GrupYoklamasi> grupYoklamalari,
        /** Haftanın günü: 1 = Pazartesi … 7 = Pazar. */
        List<GunSatiri> gunler,
        List<OgrenciSatiri> devamsizlar
) {
    public record GrupYoklamasi(Long grupId, String grupAd, long aktifOgrenci, long yoklamaGunu, LocalDate sonYoklama) {
    }

    public record GrupSatiri(Long grupId, String grupAd, long geldi, long gelmedi, long izinli, BigDecimal devamOrani) {
    }

    public record GunSatiri(int gun, long geldi, long gelmedi, long izinli) {
    }

    public record OgrenciSatiri(Long ogrenciId, String adSoyad, String grupAd, long gelmedi, long toplam, BigDecimal devamOrani) {
    }
}
