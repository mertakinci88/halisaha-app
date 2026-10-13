package com.msc.halisaha.dto;

import com.msc.halisaha.entity.OgrenciTransfer;
import com.msc.halisaha.entity.TransferDurum;

import java.time.LocalDateTime;

public record TransferResponse(
        Long id,
        Long ogrenciId,
        String ogrenciAd,
        Long kaynakGrupId,
        String kaynakGrupAd,
        Long hedefGrupId,
        String hedefGrupAd,
        TransferDurum durum,
        String aciklama,
        String talepEden,
        LocalDateTime talepTarihi,
        String kararVeren,
        /** Onay / red / iptal tarihi; bekleyen talepte null. */
        LocalDateTime kararTarihi,
        String kararNotu
) {
    public static TransferResponse from(OgrenciTransfer t) {
        return new TransferResponse(
                t.getId(),
                t.getOgrenci().getId(),
                t.getOgrenci().getAdSoyad(),
                t.getKaynakGrup() != null ? t.getKaynakGrup().getId() : null,
                t.getKaynakGrupAd(),
                t.getHedefGrup() != null ? t.getHedefGrup().getId() : null,
                t.getHedefGrupAd(),
                t.getDurum(),
                t.getAciklama(),
                t.getTalepEden() != null ? t.getTalepEden().getAdSoyad() : null,
                t.getTalepTarihi(),
                t.getKararVeren() != null ? t.getKararVeren().getAdSoyad() : null,
                t.getKararTarihi(),
                t.getKararNotu()
        );
    }
}
