package com.msc.halisaha.dto;

import com.msc.halisaha.entity.Hesap;
import com.msc.halisaha.entity.HesapDurum;

public record HesapResponse(Long id, String ad, String iban, String hesapSahibi, HesapDurum durum) {

    public static HesapResponse from(Hesap hesap) {
        return new HesapResponse(hesap.getId(), hesap.getAd(), hesap.getIban(), hesap.getHesapSahibi(), hesap.getDurum());
    }
}
