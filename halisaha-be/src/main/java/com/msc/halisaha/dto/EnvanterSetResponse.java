package com.msc.halisaha.dto;

import com.msc.halisaha.entity.EnvanterSet;
import com.msc.halisaha.entity.Envanter;
import com.msc.halisaha.entity.EnvanterBeden;
import com.msc.halisaha.entity.EnvanterTur;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record EnvanterSetResponse(
        Long id,
        String ad,
        BigDecimal toplamTutar,
        LocalDateTime kayitTarihi,
        LocalDateTime guncellemeTarihi,
        String kaydeden,
        List<KalemResponse> kalemler
) {

    public record KalemResponse(Long envanterId, EnvanterTur tur, String turAd, String beden, Integer adet, BigDecimal satisFiyati) {

        static KalemResponse from(Envanter e) {
            return new KalemResponse(e.getId(), e.getTur(), e.getTur().getAd(), e.getBeden(), e.getAdet(), e.getSatisFiyati());
        }
    }

    public static EnvanterSetResponse from(EnvanterSet s) {
        List<KalemResponse> kalemler = s.getEnvanterler().stream()
                .sorted(Comparator.comparingInt((Envanter e) -> e.getTur().ordinal())
                        .thenComparingInt(e -> EnvanterBeden.sira(e.getBeden())))
                .map(KalemResponse::from)
                .toList();
        BigDecimal toplam = kalemler.stream().map(KalemResponse::satisFiyati).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new EnvanterSetResponse(s.getId(), s.getAd(), toplam, s.getKayitTarihi(), s.getGuncellemeTarihi(),
                s.getKullanici() != null ? s.getKullanici().getAdSoyad() : null, kalemler);
    }
}
