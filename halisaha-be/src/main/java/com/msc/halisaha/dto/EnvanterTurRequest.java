package com.msc.halisaha.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record EnvanterTurRequest(
        @NotBlank(message = "Envanter adı zorunludur") @Size(max = 60, message = "Envanter adı en fazla 60 karakter olabilir") String ad,
        @NotNull(message = "Bedenli bilgisi zorunludur") Boolean bedenli,
        @NotNull(message = "Birim zorunludur") @Pattern(regexp = "adet|çift", message = "Birim adet ya da çift olmalıdır") String birim,
        /** Bedenli türlerde en az bir beden gerekir (en fazla 10 karakter); bedensiz türlerde yok sayılır. */
        List<String> bedenler
) {
}
