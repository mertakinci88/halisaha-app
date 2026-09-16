package com.msc.halisaha.dto;

import com.msc.halisaha.entity.Modul;
import com.msc.halisaha.entity.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record KullaniciOlusturRequest(
        @NotBlank(message = "Kullanıcı adı zorunludur") String kullaniciAdi,
        @NotBlank(message = "Şifre zorunludur") @Size(min = 6, message = "Şifre en az 6 karakter olmalıdır") String sifre,
        @NotBlank(message = "Ad soyad zorunludur") String adSoyad,
        @NotNull(message = "Rol zorunludur") Rol rol,
        Set<Modul> yetkiler
) {
}
