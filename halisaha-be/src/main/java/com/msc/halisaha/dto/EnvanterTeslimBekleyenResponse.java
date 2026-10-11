package com.msc.halisaha.dto;

import com.msc.halisaha.entity.EnvanterSatisKalem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Paneldeki envanter kartı için: ödemesi alınmış ama teslim edilmemiş envanterler, öğrenciye göre gruplu. */
public record EnvanterTeslimBekleyenResponse(
        int kalemSayisi,
        List<OgrenciResponse> ogrenciler
) {

    public record OgrenciResponse(Long ogrenciId, String adSoyad, List<KalemResponse> kalemler) {
    }

    /** stokYok: envanterin stoğu bitmiş, teslim için önce stok girişi gerekir. */
    public record KalemResponse(String turAd, String beden, boolean bedenli, boolean stokYok) {
    }

    public static EnvanterTeslimBekleyenResponse from(List<EnvanterSatisKalem> kalemler) {
        Map<Long, OgrenciResponse> gruplar = new LinkedHashMap<>();
        for (EnvanterSatisKalem k : kalemler) {
            var ogrenci = k.getSatis().getOgrenci();
            gruplar.computeIfAbsent(ogrenci.getId(), id -> new OgrenciResponse(id, ogrenci.getAdSoyad(), new ArrayList<>()))
                    .kalemler().add(new KalemResponse(k.getEnvanter().getTur().getAd(), k.getEnvanter().getBeden(),
                            k.getEnvanter().getTur().isBedenli(), k.getEnvanter().getAdet() <= 0));
        }
        return new EnvanterTeslimBekleyenResponse(kalemler.size(), new ArrayList<>(gruplar.values()));
    }
}
