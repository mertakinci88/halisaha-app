package com.msc.halisaha.dto;

import com.msc.halisaha.entity.Isletme;
import com.msc.halisaha.entity.IsletmeDurum;

import java.time.LocalDateTime;

public record IsletmeResponse(
        Long id,
        String ad,
        String yoneticiAdSoyad,
        String yoneticiTelNo,
        IsletmeDurum durum,
        LocalDateTime kayitTarih
) {

    public static IsletmeResponse from(Isletme isletme) {
        return new IsletmeResponse(isletme.getId(), isletme.getAd(), isletme.getYoneticiAdSoyad(), isletme.getYoneticiTelNo(), isletme.getDurum(), isletme.getKayitTarih());
    }
}
