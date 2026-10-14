package com.msc.halisaha.dto;

import com.msc.halisaha.entity.Iade;
import com.msc.halisaha.entity.IadeKaynak;
import com.msc.halisaha.entity.OdemeYontemi;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record IadeResponse(
        Long id,
        IadeKaynak kaynak,
        Long kaynakId,
        BigDecimal tutar,
        OdemeYontemi iadeSekli,
        Long hesapId,
        String hesapAd,
        LocalDate iadeTarihi,
        String neden,
        String kaydedenAd,
        LocalDateTime kayitTarih
) {

    public static IadeResponse from(Iade i) {
        IadeKaynak kaynak;
        Long kaynakId;
        if (i.getRezervasyon() != null) {
            kaynak = IadeKaynak.REZERVASYON;
            kaynakId = i.getRezervasyon().getId();
        } else if (i.getOgrenciOdeme() != null) {
            kaynak = IadeKaynak.OGRENCI_ODEME;
            kaynakId = i.getOgrenciOdeme().getId();
        } else if (i.getEnvanterSatis() != null) {
            kaynak = IadeKaynak.ENVANTER_SATIS;
            kaynakId = i.getEnvanterSatis().getId();
        } else {
            kaynak = IadeKaynak.ABONELIK;
            kaynakId = i.getAbonelik().getId();
        }
        return new IadeResponse(i.getId(), kaynak, kaynakId, i.getTutar(), i.getIadeSekli(),
                i.getHesap() != null ? i.getHesap().getId() : null,
                i.getHesap() != null ? i.getHesap().getAd() : null,
                i.getIadeTarihi(), i.getNeden(),
                i.getKullanici() != null ? i.getKullanici().getAdSoyad() : null,
                i.getKayitTarih());
    }
}
