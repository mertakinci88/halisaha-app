package com.msc.halisaha.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Kayıttan önce: bu seçimle kaç rezervasyon oluşacağı ve hangi tarihlerde çakışma olduğu. */
public record AbonelikOnizlemeRequest(
        @NotNull(message = "Saha seçimi zorunludur") Long sahaId,
        @NotNull(message = "Gün seçimi zorunludur") @Min(1) @Max(7) Integer haftaGunu,
        @NotNull(message = "Saat seçimi zorunludur") @Min(720) @Max(1530) Integer baslangicDakika,
        @NotNull(message = "Süre zorunludur") @Min(30) Integer sureDakika,
        @NotNull(message = "Başlangıç tarihi zorunludur") LocalDate baslangicTarih
) {
}
