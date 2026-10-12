package com.msc.halisaha.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OgrenciOdemeKaydetRequest(
        @NotNull(message = "Aylar zorunludur") List<@Valid @NotNull OgrenciOdemeAyRequest> aylar
) {
}
