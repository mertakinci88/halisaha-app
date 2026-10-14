package com.msc.halisaha.dto;

import com.msc.halisaha.entity.Abonelik;
import com.msc.halisaha.entity.AbonelikDurum;
import com.msc.halisaha.entity.AbonelikOdeme;
import com.msc.halisaha.entity.OdemeDurumu;
import com.msc.halisaha.entity.OdemeYontemi;
import com.msc.halisaha.entity.RezervasyonDurum;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AbonelikResponse(
        Long id,
        Long sahaId,
        String sahaAd,
        String adSoyad,
        String telNo,
        int haftaGunu,
        /** Gün başından itibaren dakika (12:00 = 720). */
        int baslangicDakika,
        int sureDakika,
        LocalDate baslangicTarih,
        LocalDate bitisTarih,
        AbonelikDurum durum,
        /** Durum aktif olduğu halde bitiş tarihi geçmişse true. */
        boolean suresiDoldu,
        BigDecimal ucret,
        BigDecimal odenenTutar,
        BigDecimal kalanTutar,
        OdemeDurumu odemeDurumu,
        String not,
        LocalDateTime iptalTarih,
        LocalDateTime kayitTarih,
        String kaydedenAd,
        List<OdemeResponse> odemeler,
        /** Yalnızca detayda dolu: aboneliğin haftalık rezervasyonları (tarih sırasıyla). */
        List<HaftaResponse> haftalar
) {

    public record OdemeResponse(Long id, BigDecimal tutar, OdemeYontemi odemeSekli, Long hesapId, String hesapAd,
                                LocalDate odemeTarihi, String kaydedenAd) {

        static OdemeResponse from(AbonelikOdeme o) {
            return new OdemeResponse(o.getId(), o.getTutar(), o.getOdemeSekli(),
                    o.getHesap() != null ? o.getHesap().getId() : null,
                    o.getHesap() != null ? o.getHesap().getAd() : null,
                    o.getOdemeTarihi(),
                    o.getKullanici() != null ? o.getKullanici().getAdSoyad() : null);
        }
    }

    public record HaftaResponse(Long rezervasyonId, LocalDateTime baslangicTarih, LocalDateTime bitisTarih, RezervasyonDurum durum) {
    }

    public static AbonelikResponse from(Abonelik a, LocalDate bugun, List<HaftaResponse> haftalar) {
        BigDecimal odenen = a.getOdemeler().stream().map(AbonelikOdeme::getTutar).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AbonelikResponse(
                a.getId(), a.getSaha().getId(), a.getSaha().getAd(), a.getAdSoyad(), a.getTelNo(),
                a.getHaftaGunu(), a.getBaslangicDakika(), a.getSureDakika(), a.getBaslangicTarih(), a.getBitisTarih(),
                a.getDurum(),
                a.getDurum() == AbonelikDurum.AKTIF && a.getBitisTarih().isBefore(bugun),
                a.getUcret(), odenen, a.getUcret().subtract(odenen).max(BigDecimal.ZERO),
                OdemeDurumu.hesapla(odenen, a.getUcret()),
                a.getNot(), a.getIptalTarih(), a.getKayitTarih(),
                a.getKullanici() != null ? a.getKullanici().getAdSoyad() : null,
                a.getOdemeler().stream().map(OdemeResponse::from).toList(),
                haftalar);
    }
}
