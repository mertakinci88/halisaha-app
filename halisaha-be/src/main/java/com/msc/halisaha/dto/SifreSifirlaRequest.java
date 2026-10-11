package com.msc.halisaha.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SifreSifirlaRequest(
        @NotBlank(message = "Şifre zorunludur") @Size(min = 6, message = "Şifre en az 6 karakter olmalıdır") String sifre
) {
}
