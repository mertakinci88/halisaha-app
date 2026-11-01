package com.msc.halisaha.dto;

import com.msc.halisaha.entity.EnvanterSet;
import com.msc.halisaha.entity.Envanter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public record EnvanterSetResponse(
        Long id,
        String ad,
        String beden,
        BigDecimal tutar,
        LocalDateTime kayitTarihi,
        LocalDateTime guncellemeTarihi,
        String kaydeden,
        List<KalemResponse> kalemler
) {

    private static final Collator TR = Collator.getInstance(Locale.forLanguageTag("tr"));

    /** Setin kalemi: fiyat içermez (set tutarı sette tutulur). */
    public record KalemResponse(Long envanterId, Long turId, String turAd, String beden, Integer adet) {

        static KalemResponse from(Envanter e) {
            return new KalemResponse(e.getId(), e.getTur().getId(), e.getTur().getAd(), e.getBeden(), e.getAdet());
        }
    }

    public static EnvanterSetResponse from(EnvanterSet s) {
        List<KalemResponse> kalemler = s.getEnvanterler().stream()
                .sorted(Comparator.comparing((Envanter e) -> e.getTur().getAd(), TR)
                        .thenComparingInt(e -> e.getTur().bedenSira(e.getBeden())))
                .map(KalemResponse::from)
                .toList();
        return new EnvanterSetResponse(s.getId(), s.getAd(), s.getBeden(), s.getTutar(), s.getKayitTarihi(), s.getGuncellemeTarihi(),
                s.getKullanici() != null ? s.getKullanici().getAdSoyad() : null, kalemler);
    }
}
