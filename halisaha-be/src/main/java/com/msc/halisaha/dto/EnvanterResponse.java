package com.msc.halisaha.dto;

import com.msc.halisaha.entity.Envanter;
import com.msc.halisaha.entity.EnvanterTur;

import java.math.BigDecimal;

public record EnvanterResponse(
        Long id,
        EnvanterTur tur,
        String turAd,
        boolean bedenli,
        String beden,
        Integer adet,
        Integer stokUyariAdedi,
        BigDecimal satisFiyati
) {

    public static EnvanterResponse from(Envanter e) {
        return new EnvanterResponse(e.getId(), e.getTur(), e.getTur().getAd(), e.getTur().isBedenli(),
                e.getBeden(), e.getAdet(), e.getStokUyariAdedi(), e.getSatisFiyati());
    }
}
