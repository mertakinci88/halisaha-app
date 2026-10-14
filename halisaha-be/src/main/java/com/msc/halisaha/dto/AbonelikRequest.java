package com.msc.halisaha.dto;

import com.msc.halisaha.entity.OdemeYontemi;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Yeni abonelik: sahada haftanın belli günü ve saatinde, başlangıç tarihinden 1 yıl boyunca her hafta için rezervasyon oluşturulur.
 * Bitiş tarihi gönderilmez, başlangıçtan 1 yıl sonrası olarak hesaplanır. Kayıtta ilk ödeme de alınır.
 */
public record AbonelikRequest(
        @NotNull(message = "Saha seçimi zorunludur") Long sahaId,
        @NotBlank(message = "Ad soyad zorunludur") @Size(max = 255, message = "Ad soyad en fazla 255 karakter olabilir") String adSoyad,
        @NotBlank(message = "Telefon numarası zorunludur")
        @Pattern(regexp = "^05\\d{9}$", message = "Telefon numarası 11 haneli olmalı ve 05 ile başlamalıdır (örn. 05469764613)")
        String telNo,
        @NotNull(message = "Gün seçimi zorunludur") @Min(value = 1, message = "Gün 1-7 arasında olmalıdır") @Max(value = 7, message = "Gün 1-7 arasında olmalıdır")
        Integer haftaGunu,
        @NotNull(message = "Saat seçimi zorunludur") @Min(value = 12, message = "Başlangıç saati 12:00 - 01:00 arasında olmalıdır") @Max(value = 25, message = "Başlangıç saati 12:00 - 01:00 arasında olmalıdır")
        Integer baslangicSaat,
        @NotNull(message = "Süre zorunludur") @Min(value = 1, message = "Süre en az 1 saat olmalıdır") Integer sureSaat,
        @NotNull(message = "Başlangıç tarihi zorunludur") LocalDate baslangicTarih,
        /** Abonelik için anlaşılan toplam tutar (standart ücretten gelir, elle değiştirilebilir). */
        @NotNull(message = "Abonelik ücreti zorunludur") @DecimalMin(value = "0.01", message = "Abonelik ücreti sıfırdan büyük olmalıdır") BigDecimal ucret,
        /** Kayıtta alınan ödeme; ücreti aşamaz. */
        @NotNull(message = "Alınan ödeme tutarı zorunludur") @DecimalMin(value = "0.01", message = "Alınan ödeme sıfırdan büyük olmalıdır") BigDecimal odenenTutar,
        @NotNull(message = "Ödeme şekli seçilmelidir") OdemeYontemi odemeSekli,
        /** Yalnızca Havale / EFT'de dikkate alınır ve o durumda zorunludur. */
        Long hesapId,
        /** Boşsa bugün. */
        LocalDate odemeTarihi,
        @Size(max = 255, message = "Not en fazla 255 karakter olabilir") String not
) {
}
