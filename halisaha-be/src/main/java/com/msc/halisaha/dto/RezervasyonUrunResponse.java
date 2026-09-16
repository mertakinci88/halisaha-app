package com.msc.halisaha.dto;

import java.math.BigDecimal;

public record RezervasyonUrunResponse(
        Long id,
        Long urunId,
        String urunAd,
        Integer miktar,
        BigDecimal birimFiyat,
        BigDecimal araToplam
) {
}
