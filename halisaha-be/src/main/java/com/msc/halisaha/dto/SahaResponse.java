package com.msc.halisaha.dto;

import com.msc.halisaha.entity.Saha;
import com.msc.halisaha.entity.SahaDurum;

import java.math.BigDecimal;

public record SahaResponse(Long id, String ad, SahaDurum durum, BigDecimal saatlikUcret) {

    public static SahaResponse from(Saha saha) {
        return new SahaResponse(saha.getId(), saha.getAd(), saha.getDurum(), saha.getSaatlikUcret());
    }
}
