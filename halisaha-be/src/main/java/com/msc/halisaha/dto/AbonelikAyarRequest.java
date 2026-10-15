package com.msc.halisaha.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AbonelikAyarRequest(
        @NotNull(message = "Standart depozito zorunludur") @DecimalMin(value = "0.00", message = "Depozito negatif olamaz") BigDecimal standartUcret
) {
}
