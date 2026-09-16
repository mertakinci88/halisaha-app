package com.msc.halisaha.dto;

import com.msc.halisaha.entity.OdemeDurumu;
import com.msc.halisaha.entity.OdemeYontemi;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record RezervasyonRequest(
        @NotNull(message = "Saha seçimi zorunludur") Long sahaId,
        @NotBlank(message = "Ad soyad zorunludur") String adSoyad,
        @NotNull(message = "Başlangıç tarihi zorunludur") LocalDateTime baslangicTarih,
        @NotNull(message = "Bitiş tarihi zorunludur") LocalDateTime bitisTarih,
        @NotBlank(message = "Telefon numarası zorunludur") String telNo,
        OdemeYontemi odemeYontemi,
        OdemeDurumu odemeDurumu
) {
}
