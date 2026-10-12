package com.msc.halisaha.dto;

import com.msc.halisaha.entity.OdemeTipi;
import com.msc.halisaha.entity.OdemeYontemi;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OdemeRequest(
        @NotNull(message = "Ödeme tipi zorunludur") OdemeTipi tip,
        @NotNull(message = "Tutar zorunludur") @DecimalMin(value = "0.01", message = "Tutar sıfırdan büyük olmalıdır") BigDecimal tutar,
        @NotNull(message = "Ödeme yöntemi zorunludur") OdemeYontemi yontem,
        /** Yalnızca Havale / EFT'de dikkate alınır. */
        Long hesapId
) {
}
