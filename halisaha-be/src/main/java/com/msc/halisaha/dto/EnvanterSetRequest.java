package com.msc.halisaha.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Set tanımı: ad ve envanter kayıtları (tür + beden). Her türden en fazla bir kayıt seçilebilir. */
public record EnvanterSetRequest(
        @NotBlank(message = "Set adı zorunludur") @Size(max = 100, message = "Set adı en fazla 100 karakter olabilir") String ad,
        @NotNull(message = "Envanter listesi zorunludur") List<Long> envanterIdleri
) {
}
