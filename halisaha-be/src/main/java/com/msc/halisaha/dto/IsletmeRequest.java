package com.msc.halisaha.dto;

import jakarta.validation.constraints.NotBlank;

public record IsletmeRequest(
        @NotBlank(message = "İşletme adı zorunludur") String ad,
        String yoneticiAdSoyad,
        String yoneticiTelNo
) {
}
