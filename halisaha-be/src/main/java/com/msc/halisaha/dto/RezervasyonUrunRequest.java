package com.msc.halisaha.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RezervasyonUrunRequest(
        @NotNull(message = "Ürün seçimi zorunludur") Long urunId,
        @NotNull(message = "Miktar zorunludur") @Positive(message = "Miktar pozitif olmalıdır") Integer miktar
) {
}
