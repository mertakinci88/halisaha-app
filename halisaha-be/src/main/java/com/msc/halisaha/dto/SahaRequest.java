package com.msc.halisaha.dto;

import com.msc.halisaha.entity.SahaDurum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SahaRequest(
        @NotBlank(message = "Saha adı zorunludur") String ad,
        SahaDurum durum,
        @NotNull(message = "Saatlik ücret zorunludur") @Positive(message = "Saatlik ücret pozitif olmalıdır") BigDecimal saatlikUcret
) {
}
