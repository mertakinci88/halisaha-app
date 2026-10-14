package com.msc.halisaha.dto;

import jakarta.validation.constraints.Size;

/** Rezervasyon / abonelik iptalinde isteğe bağlı neden. */
public record IptalRequest(@Size(max = 255, message = "Neden en fazla 255 karakter olabilir") String neden) {
}
