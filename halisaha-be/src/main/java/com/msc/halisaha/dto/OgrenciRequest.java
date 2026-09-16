package com.msc.halisaha.dto;

import com.msc.halisaha.entity.OdemeDurumu;
import com.msc.halisaha.entity.OdemeYontemi;
import com.msc.halisaha.entity.OgrenciDurum;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OgrenciRequest(
        Long grupId,
        @NotBlank(message = "Ad soyad zorunludur") String adSoyad,
        LocalDate dogumTarih,
        String veliAdSoyad,
        String veliTelNo,
        OgrenciDurum durum,
        Integer antrenmanGunSayisi,
        BigDecimal aylikAidat,
        OdemeDurumu odemeDurumu,
        String odemePlani,
        String odenenDonem,
        LocalDate odemeTarihi,
        LocalDate sonrakiOdemeTarih,
        BigDecimal odenenTutar,
        OdemeYontemi odemeSekli,
        String not
) {
}
