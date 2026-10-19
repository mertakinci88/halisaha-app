package com.msc.halisaha.dto;

import com.msc.halisaha.entity.OdemeYontemi;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Bir ayda girilmiş tek bir ödeme (iade bu kayda bağlanır). */
public record OgrenciOdemeResponse(
        Long id,
        BigDecimal odenenTutar,
        boolean tamam,
        OdemeYontemi odemeSekli,
        Long hesapId,
        String hesapAd,
        LocalDate odemeTarihi,
        String not
) {
}
