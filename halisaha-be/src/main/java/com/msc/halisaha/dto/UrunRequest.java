package com.msc.halisaha.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record UrunRequest(
        @NotBlank(message = "Ürün adı zorunludur") String ad,
        @NotNull(message = "Adet zorunludur") @PositiveOrZero(message = "Adet negatif olamaz") Integer adet,
        @NotNull(message = "Fiyat zorunludur") @Positive(message = "Fiyat pozitif olmalıdır") BigDecimal fiyat
) {
}
