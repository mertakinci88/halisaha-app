package com.msc.halisaha.dto;

import com.msc.halisaha.entity.Rol;
import jakarta.validation.constraints.NotNull;

public record RolGuncelleRequest(@NotNull(message = "Rol zorunludur") Rol rol) {
}
