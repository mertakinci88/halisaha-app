package com.msc.halisaha.dto;

/** Öğrenci yoklama geçmişinin bir sayfası; durum sayıları sayfadan bağımsız, tüm kayıtlar üzerindendir. */
public record OgrenciYoklamaSayfa(
        PageResponse<OgrenciYoklamaSatir> sayfa,
        long geldi,
        long gelmedi,
        long izinli
) {
}
