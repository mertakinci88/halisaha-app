package com.msc.halisaha.dto;

import java.time.LocalDate;
import java.util.List;

public record AbonelikOnizlemeResponse(
        LocalDate baslangicTarih,
        LocalDate bitisTarih,
        /** Oluşacak rezervasyon sayısı (haftalık). */
        int rezervasyonSayisi,
        LocalDate ilkTarih,
        LocalDate sonTarih,
        /** Saha için o saatte zaten aktif rezervasyonu olan tarihler. */
        List<Cakisma> cakismalar
) {
    public record Cakisma(LocalDate tarih, String adSoyad) {
    }
}
