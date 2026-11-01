package com.msc.halisaha.dto;

import com.msc.halisaha.entity.Envanter;

import java.math.BigDecimal;

public record EnvanterResponse(
        Long id,
        Long turId,
        String turAd,
        boolean bedenli,
        String birim,
        String beden,
        Integer adet,
        Integer stokUyariAdedi,
        BigDecimal satisFiyati
) {

    public static EnvanterResponse from(Envanter e) {
        return new EnvanterResponse(e.getId(), e.getTur().getId(), e.getTur().getAd(), e.getTur().isBedenli(),
                e.getTur().getBirim(), e.getBeden(), e.getAdet(), e.getStokUyariAdedi(), e.getSatisFiyati());
    }
}
