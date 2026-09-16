package com.msc.halisaha.dto;

import com.msc.halisaha.entity.Modul;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record YetkiGuncelleRequest(@NotNull(message = "Yetki listesi zorunludur") Set<Modul> yetkiler) {
}
