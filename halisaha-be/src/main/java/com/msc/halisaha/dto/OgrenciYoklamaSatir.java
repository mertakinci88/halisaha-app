package com.msc.halisaha.dto;

import com.msc.halisaha.entity.Yoklama;
import com.msc.halisaha.entity.YoklamaDurum;

import java.time.LocalDate;

/** Öğrenci detayındaki yoklama geçmişinin bir günü. */
public record OgrenciYoklamaSatir(
        LocalDate tarih,
        YoklamaDurum durum,
        String not,
        String kaydeden
) {
    public static OgrenciYoklamaSatir from(Yoklama y) {
        return new OgrenciYoklamaSatir(y.getTarih(), y.getDurum(), y.getAciklama(),
                y.getKullanici() != null ? y.getKullanici().getAdSoyad() : null);
    }
}
