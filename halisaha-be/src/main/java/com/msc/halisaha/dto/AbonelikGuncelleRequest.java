package com.msc.halisaha.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Abonelik bilgileri: gün, saat ve tarihler değişmez (değişiklik için iptal edip yeni abonelik açılır). */
public record AbonelikGuncelleRequest(
        @NotBlank(message = "Ad soyad zorunludur") @Size(max = 255, message = "Ad soyad en fazla 255 karakter olabilir") String adSoyad,
        @NotBlank(message = "Telefon numarası zorunludur")
        @Pattern(regexp = "^05\\d{9}$", message = "Telefon numarası 11 haneli olmalı ve 05 ile başlamalıdır (örn. 05469764613)")
        String telNo,
        /** Alınan ödemenin altına düşemez. */
        @NotNull(message = "Abonelik ücreti zorunludur") @DecimalMin(value = "0.01", message = "Abonelik ücreti sıfırdan büyük olmalıdır") BigDecimal ucret,
        @Size(max = 255, message = "Not en fazla 255 karakter olabilir") String not
) {
}
