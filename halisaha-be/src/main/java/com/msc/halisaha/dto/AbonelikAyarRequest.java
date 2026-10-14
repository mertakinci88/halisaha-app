package com.msc.halisaha.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AbonelikAyarRequest(
        @NotNull(message = "Standart abonelik ücreti zorunludur") @DecimalMin(value = "0.00", message = "Ücret negatif olamaz") BigDecimal standartUcret
) {
}
