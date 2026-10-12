package com.msc.halisaha.dto;

import com.msc.halisaha.entity.OdemeYontemi;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Bir ayın ödemesi. Ödenen tutar boş ya da 0 ise o ayın kaydı silinir (ödenmedi). */
public record OgrenciOdemeAyRequest(
        @NotNull(message = "Ay zorunludur") @Min(value = 1, message = "Ay 1-12 arasında olmalıdır") @Max(value = 12, message = "Ay 1-12 arasında olmalıdır") Integer ay,
        @PositiveOrZero(message = "Ödenen tutar negatif olamaz") BigDecimal odenenTutar,
        OdemeYontemi odemeSekli,
        /** Yalnızca Havale / EFT'de dikkate alınır. */
        Long hesapId,
        LocalDate odemeTarihi,
        @Size(max = 255, message = "Not en fazla 255 karakter olabilir") String not
) {
}
