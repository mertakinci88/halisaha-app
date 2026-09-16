package com.msc.halisaha.dto;

import jakarta.validation.constraints.NotBlank;

public record GrupRequest(
        @NotBlank(message = "Grup adı zorunludur") String ad
) {
}
