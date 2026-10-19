package com.msc.halisaha.dto;

import com.msc.halisaha.entity.OgrenciOdemeDurumu;

import java.math.BigDecimal;
import java.util.List;

/** Bir ayın ödeme özeti: durum, o aya girilen ödemelerin toplamı ve ödemeler (eskiden yeniye; hiç ödeme yoksa boş liste). */
public record OgrenciOdemeAyResponse(
        int yil,
        int ay,
        OgrenciOdemeDurumu durum,
        BigDecimal odenenTutar,
        List<OgrenciOdemeResponse> odemeler
) {
}
