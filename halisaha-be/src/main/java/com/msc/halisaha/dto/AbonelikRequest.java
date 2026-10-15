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
 * Yeni abonelik: sahada haftanın belli günü ve saatinde, başlangıç tarihinden itibaren 52 hafta boyunca her hafta için rezervasyon oluşturulur.
 * Bitiş tarihi gönderilmez, 52. haftanın tarihi olarak hesaplanır. Kayıtta depozito (ya da ilk kısmı) alınır; her haftalık rezervasyon haftalık ücretle açılır.
 */
public record AbonelikRequest(
        @NotNull(message = "Saha seçimi zorunludur") Long sahaId,
        @NotBlank(message = "Ad soyad zorunludur") @Size(max = 255, message = "Ad soyad en fazla 255 karakter olabilir") String adSoyad,
        @NotBlank(message = "Telefon numarası zorunludur")
        @Pattern(regexp = "^05\\d{9}$", message = "Telefon numarası 11 haneli olmalı ve 05 ile başlamalıdır (örn. 05469764613)")
        String telNo,
        @NotNull(message = "Gün seçimi zorunludur") @Min(value = 1, message = "Gün 1-7 arasında olmalıdır") @Max(value = 7, message = "Gün 1-7 arasında olmalıdır")
        Integer haftaGunu,
        /** Gün başından itibaren dakika (12:00 = 720, 20:30 = 1230, 01:30 ertesi gün = 1530); tam saat ya da buçuklu olmalıdır. */
        @NotNull(message = "Saat seçimi zorunludur") @Min(value = 720, message = "Başlangıç saati 12:00 ile 01:30 arasında olmalıdır") @Max(value = 1530, message = "Başlangıç saati 12:00 ile 01:30 arasında olmalıdır")
        Integer baslangicDakika,
        /** Dakika; 30'un katı (1 saat = 60). */
        @NotNull(message = "Süre zorunludur") @Min(value = 30, message = "Süre en az 30 dakika olmalıdır") Integer sureDakika,
        @NotNull(message = "Başlangıç tarihi zorunludur") LocalDate baslangicTarih,
        /** Bırakılan depozito (standart depozitodan gelir, elle değiştirilebilir). */
        @NotNull(message = "Depozito zorunludur") @DecimalMin(value = "0.01", message = "Depozito sıfırdan büyük olmalıdır") BigDecimal depozito,
        /** Maç başına ücret (sahanın ücretinden gelir, elle değiştirilebilir). */
        @NotNull(message = "Haftalık ücret zorunludur") @DecimalMin(value = "0.01", message = "Haftalık ücret sıfırdan büyük olmalıdır") BigDecimal haftalikUcret,
        /** Kayıtta alınan depozito ödemesi; depozitoyu aşamaz. */
        @NotNull(message = "Alınan ödeme tutarı zorunludur") @DecimalMin(value = "0.01", message = "Alınan ödeme sıfırdan büyük olmalıdır") BigDecimal odenenTutar,
        @NotNull(message = "Ödeme şekli seçilmelidir") OdemeYontemi odemeSekli,
        /** Yalnızca Havale / EFT'de dikkate alınır ve o durumda zorunludur. */
        Long hesapId,
        /** Boşsa bugün. */
        LocalDate odemeTarihi,
        @Size(max = 255, message = "Not en fazla 255 karakter olabilir") String not
) {
}
