package com.msc.halisaha.dto;

import com.msc.halisaha.entity.OdemeDurumu;
import com.msc.halisaha.entity.OdemeYontemi;
import com.msc.halisaha.entity.Ogrenci;
import com.msc.halisaha.entity.OgrenciDurum;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record OgrenciResponse(
        Long id,
        Long grupId,
        String grupAd,
        String adSoyad,
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
        LocalDateTime kayitTarih,
        String not
) {
    public static OgrenciResponse from(Ogrenci ogrenci) {
        return new OgrenciResponse(
                ogrenci.getId(),
                ogrenci.getGrup() != null ? ogrenci.getGrup().getId() : null,
                ogrenci.getGrup() != null ? ogrenci.getGrup().getAd() : null,
                ogrenci.getAdSoyad(),
                ogrenci.getDogumTarih(),
                ogrenci.getVeliAdSoyad(),
                ogrenci.getVeliTelNo(),
                ogrenci.getDurum(),
                ogrenci.getAntrenmanGunSayisi(),
                ogrenci.getAylikAidat(),
                ogrenci.getOdemeDurumu(),
                ogrenci.getOdemePlani(),
                ogrenci.getOdenenDonem(),
                ogrenci.getOdemeTarihi(),
                ogrenci.getSonrakiOdemeTarih(),
                ogrenci.getOdenenTutar(),
                ogrenci.getOdemeSekli(),
                ogrenci.getKayitTarih(),
                ogrenci.getNot()
        );
    }
}
