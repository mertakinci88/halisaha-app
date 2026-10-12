package com.msc.halisaha.dto;

import com.msc.halisaha.entity.HesapDurum;
import jakarta.validation.constraints.NotBlank;

public record HesapRequest(
        @NotBlank(message = "Hesap adı zorunludur") String ad,
        @NotBlank(message = "IBAN zorunludur") String iban,
        @NotBlank(message = "Hesap sahibi zorunludur") String hesapSahibi,
        HesapDurum durum
) {
}
