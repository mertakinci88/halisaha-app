package com.msc.halisaha.dto;

import com.msc.halisaha.entity.EnvanterSatis;
import com.msc.halisaha.entity.EnvanterSatisKalem;
import com.msc.halisaha.entity.EnvanterSatisOdeme;
import com.msc.halisaha.entity.EnvanterTur;
import com.msc.halisaha.entity.OdemeDurumu;
import com.msc.halisaha.entity.OdemeYontemi;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record EnvanterSatisResponse(
        Long id,
        Long ogrenciId,
        Long setId,
        String setAd,
        BigDecimal setTutari,
        boolean sabitTutar,
        BigDecimal odenenTutar,
        BigDecimal kalanTutar,
        OdemeDurumu odemeDurumu,
        LocalDateTime kayitTarihi,
        List<KalemResponse> kalemler,
        List<OdemeResponse> odemeler
) {

    public record OdemeResponse(Long id, BigDecimal tutar, OdemeYontemi odemeSekli, Long hesapId, String hesapAd, LocalDateTime odemeTarihi) {

        static OdemeResponse from(EnvanterSatisOdeme o) {
            return new OdemeResponse(o.getId(), o.getTutar(), o.getOdemeSekli(),
                    o.getHesap() != null ? o.getHesap().getId() : null,
                    o.getHesap() != null ? o.getHesap().getAd() : null,
                    o.getOdemeTarihi());
        }
    }


    public record KalemResponse(
            EnvanterTur tur,
            String turAd,
            Long envanterId,
            String beden,
            BigDecimal birimFiyat,
            boolean odendi,
            boolean teslimEdildi
    ) {

        static KalemResponse from(EnvanterSatisKalem k) {
            return new KalemResponse(k.getEnvanter().getTur(), k.getEnvanter().getTur().getAd(), k.getEnvanter().getId(),
                    k.getEnvanter().getBeden(), k.getBirimFiyat(), k.isOdendi(), k.isTeslimEdildi());
        }
    }

    public static EnvanterSatisResponse from(EnvanterSatis s) {
        return new EnvanterSatisResponse(
                s.getId(),
                s.getOgrenci().getId(),
                s.getSet() != null ? s.getSet().getId() : null,
                s.getSetAd(),
                s.getSetTutari(),
                s.isSabitTutar(),
                s.getOdenenTutar(),
                s.getSetTutari().subtract(s.getOdenenTutar()).max(BigDecimal.ZERO),
                OdemeDurumu.hesapla(s.getOdenenTutar(), s.getSetTutari()),
                s.getKayitTarihi(),
                s.getKalemler().stream()
                        .sorted(Comparator.comparing((EnvanterSatisKalem k) -> k.getEnvanter().getTur().ordinal()))
                        .map(KalemResponse::from)
                        .toList(),
                s.getOdemeler().stream().map(OdemeResponse::from).toList());
    }
}
