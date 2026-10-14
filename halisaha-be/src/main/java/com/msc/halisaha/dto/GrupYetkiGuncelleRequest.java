package com.msc.halisaha.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

/** grupKisitli=false: tüm gruplar; true: yalnızca grupIdleri'ndeki gruplar (en az biri seçilmelidir). */
public record GrupYetkiGuncelleRequest(
        @NotNull(message = "Grup kısıtı zorunludur") Boolean grupKisitli,
        Set<Long> grupIdleri
) {
}
