package com.msc.halisaha.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransferTalepRequest(
        @NotNull(message = "Hedef grup zorunludur") Long hedefGrupId,
        @Size(max = 500, message = "Açıklama en fazla 500 karakter olabilir") String aciklama
) {
}
