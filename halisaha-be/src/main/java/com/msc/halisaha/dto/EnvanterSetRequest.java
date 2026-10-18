package com.msc.halisaha.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Set tanımı: ad, beden, tutar ve seçilen envanter kayıtları. Bedenli ürünler set bedeninden, bedensiz ürünler
 * (kampon çantası, tozluk) standart kayıttan seçilir; her türden en fazla bir kayıt, en az bir kayıt gerekir.
 */
public record EnvanterSetRequest(
        @NotBlank(message = "Set adı zorunludur") @Size(max = 100, message = "Set adı en fazla 100 karakter olabilir") String ad,
        @NotBlank(message = "Beden zorunludur") String beden,
        @NotNull(message = "Set tutarı zorunludur") @DecimalMin(value = "0.01", message = "Set tutarı 0'dan büyük olmalıdır") BigDecimal tutar,
        @NotNull(message = "Envanter listesi zorunludur") List<Long> envanterIdleri
) {
}
