package com.msc.halisaha.dto;

import com.msc.halisaha.entity.OdemeYontemi;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ödemeler bu istekle değil, ödeme uç noktasıyla alınır. {@code kapora} ve {@code odemeYontemi}
 * sadece oluşturma sırasında peşin alınan kaporayı belirtir; güncellemede dikkate alınmaz.
 */
public record RezervasyonRequest(
        @NotNull(message = "Saha seçimi zorunludur") Long sahaId,
        @NotBlank(message = "Ad soyad zorunludur") String adSoyad,
        @NotNull(message = "Başlangıç tarihi zorunludur") LocalDateTime baslangicTarih,
        @NotNull(message = "Bitiş tarihi zorunludur") LocalDateTime bitisTarih,
        @NotBlank(message = "Telefon numarası zorunludur")
        @Pattern(regexp = "^05\\d{9}$", message = "Telefon numarası 11 haneli olmalı ve 05 ile başlamalıdır (örn. 05469764613)")
        String telNo,
        OdemeYontemi odemeYontemi,
        @DecimalMin(value = "0.00", message = "Kapora negatif olamaz") BigDecimal kapora,
        /** Kapora Havale / EFT ile alınıyorsa paranın yattığı hesap. */
        Long hesapId,
        /** true ise kapora yerine rezervasyon tutarının tamamı peşin tahsil edilir (tip ODEME); {@code kapora} yok sayılır. */
        Boolean tamOdeme
) {
}
