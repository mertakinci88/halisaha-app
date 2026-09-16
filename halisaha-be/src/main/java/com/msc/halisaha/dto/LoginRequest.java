package com.msc.halisaha.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Kullanıcı adı zorunludur") String kullaniciAdi,
        @NotBlank(message = "Şifre zorunludur") String sifre
) {
}
