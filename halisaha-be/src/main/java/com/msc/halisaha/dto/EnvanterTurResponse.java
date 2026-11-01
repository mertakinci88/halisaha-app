package com.msc.halisaha.dto;

import com.msc.halisaha.entity.EnvanterTur;

import java.util.List;

public record EnvanterTurResponse(Long id, String ad, boolean bedenli, String birim, List<String> bedenler, boolean kullanimda) {

    public static EnvanterTurResponse from(EnvanterTur t, boolean kullanimda) {
        return new EnvanterTurResponse(t.getId(), t.getAd(), t.isBedenli(), t.getBirim(), List.copyOf(t.getBedenler()), kullanimda);
    }
}
