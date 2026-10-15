package com.msc.halisaha.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Rezervasyonun saha ücretini (ödenecek tutarın saha kısmı) kayıttan sonra elle değiştirmek için; alınan ödemenin altına düşemez. */
public record SahaTutarRequest(
        @NotNull(message = "Ödenecek tutar zorunludur") @DecimalMin(value = "0.00", message = "Ödenecek tutar negatif olamaz") BigDecimal tutar
) {
}
