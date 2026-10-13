package com.msc.halisaha.dto;

import jakarta.validation.constraints.Size;

/** Red nedeni ya da iptal notu (isteğe bağlı). */
public record TransferKararRequest(
        @Size(max = 500, message = "Not en fazla 500 karakter olabilir") String not
) {
}
