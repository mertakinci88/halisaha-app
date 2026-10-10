package com.msc.halisaha.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CiroRaporu(
        String esas,
        String grupla,
        LocalDate baslangic,
        LocalDate bitis,
        Long sahaId,
        BigDecimal toplam,
        long adet,
        List<CiroSatir> satirlar
) {
}
