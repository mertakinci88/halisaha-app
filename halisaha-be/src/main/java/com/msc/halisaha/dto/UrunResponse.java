package com.msc.halisaha.dto;

import com.msc.halisaha.entity.Urun;

import java.math.BigDecimal;

public record UrunResponse(Long id, String ad, Integer adet, BigDecimal fiyat) {

    public static UrunResponse from(Urun urun) {
        return new UrunResponse(urun.getId(), urun.getAd(), urun.getAdet(), urun.getFiyat());
    }
}
