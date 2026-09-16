package com.msc.halisaha.dto;

import com.msc.halisaha.entity.Grup;

public record GrupResponse(Long id, String ad) {

    public static GrupResponse from(Grup grup) {
        return new GrupResponse(grup.getId(), grup.getAd());
    }
}
