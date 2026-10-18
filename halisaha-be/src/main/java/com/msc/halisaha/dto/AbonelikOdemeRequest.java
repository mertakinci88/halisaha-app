package com.msc.halisaha.dto;

import com.msc.halisaha.entity.OdemeYontemi;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AbonelikOdemeRequest(
        @NotNull(message = "Ödeme tutarı zorunludur") @DecimalMin(value = "0.01", message = "Ödeme tutarı sıfırdan büyük olmalıdır") BigDecimal tutar,
        @NotNull(message = "Ödeme yöntemi seçilmelidir") OdemeYontemi odemeSekli,
        /** Yalnızca Havale / EFT'de dikkate alınır ve o durumda zorunludur. */
        Long hesapId,
        /** Boşsa bugün. */
        LocalDate odemeTarihi
) {
}
