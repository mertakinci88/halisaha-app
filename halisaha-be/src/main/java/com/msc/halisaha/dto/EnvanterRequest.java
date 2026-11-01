package com.msc.halisaha.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record EnvanterRequest(
        @NotNull(message = "Envanter zorunludur") Long turId,
        /** Yalnızca bedenli türlerde zorunludur ve türün bedenlerinden biri olmalıdır; diğerlerinde yok sayılır. */
        String beden,
        @NotNull(message = "Adet zorunludur") @PositiveOrZero(message = "Adet negatif olamaz") Integer adet,
        @NotNull(message = "Satış fiyatı zorunludur") @Positive(message = "Satış fiyatı pozitif olmalıdır") BigDecimal satisFiyati,
        /** Boş bırakılırsa oluştururken 10, güncellerken mevcut değer korunur. */
        @PositiveOrZero(message = "Stok uyarı adedi negatif olamaz") Integer stokUyariAdedi
) {
}
