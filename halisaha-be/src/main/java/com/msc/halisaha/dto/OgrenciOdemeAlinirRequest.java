package com.msc.halisaha.dto;

import jakarta.validation.constraints.NotNull;

public record OgrenciOdemeAlinirRequest(
        @NotNull(message = "Ödeme alınıp alınmayacağı belirtilmelidir") Boolean alinir,
        /** Ödeme kaydı olan öğrencide ödeme alımı kapatılırken kullanıcının uyarıyı onayladığı. */
        Boolean onay
) {
}
