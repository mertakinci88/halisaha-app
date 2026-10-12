package com.msc.halisaha.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SifreDegistirRequest(
        @NotBlank(message = "Mevcut şifre zorunludur") String eskiSifre,
        @NotBlank(message = "Yeni şifre zorunludur") @Size(min = 6, message = "Şifre en az 6 karakter olmalıdır") String yeniSifre
) {
}
