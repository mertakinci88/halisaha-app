package com.msc.halisaha.dto;

import com.msc.halisaha.entity.YoklamaDurum;

/** Bir öğrencinin seçili günkü yoklaması; durum null ise öğrenci o gün için işaretlenmemiştir. */
public record YoklamaSatir(
        Long ogrenciId,
        String adSoyad,
        YoklamaDurum durum,
        String not
) {
}
