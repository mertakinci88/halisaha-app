package com.msc.halisaha.dto;

import com.msc.halisaha.entity.OdemeTipi;
import com.msc.halisaha.entity.OdemeYontemi;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OdemeResponse(
        Long id,
        OdemeTipi tip,
        BigDecimal tutar,
        OdemeYontemi yontem,
        LocalDateTime odemeTarihi,
        String kullaniciAdSoyad
) {
}
