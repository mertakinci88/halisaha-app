package com.msc.halisaha.dto;

import com.msc.halisaha.entity.OgrenciOdemeDurumu;
import com.msc.halisaha.entity.OdemeYontemi;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Bir ayın ödeme satırı. Ödeme girilmemiş ayda durum ODEME_BEKLIYOR (ayın 15'ine kadar, gelecek aylar dahil) ya da ODENMEDI, odenenTutar 0, odemeSekli / odemeTarihi null'dır. */
public record OgrenciOdemeAyResponse(
        /** Ödeme kaydının id'si (iade bu kayda bağlanır); ödeme girilmemiş ayda null. */
        Long id,
        int yil,
        int ay,
        BigDecimal odenenTutar,
        OgrenciOdemeDurumu durum,
        OdemeYontemi odemeSekli,
        Long hesapId,
        String hesapAd,
        LocalDate odemeTarihi,
        String not
) {
}
