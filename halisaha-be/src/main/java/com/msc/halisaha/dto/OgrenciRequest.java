package com.msc.halisaha.dto;

import com.msc.halisaha.entity.OgrenciDurum;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record OgrenciRequest(
        Long grupId,
        @NotBlank(message = "Ad soyad zorunludur") String adSoyad,
        LocalDate dogumTarih,
        String veliAdSoyad,
        String veliTelNo,
        OgrenciDurum durum,
        Integer antrenmanGunSayisi,
        String not
) {
}
