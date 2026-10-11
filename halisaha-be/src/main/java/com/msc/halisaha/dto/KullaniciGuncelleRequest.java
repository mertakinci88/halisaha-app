package com.msc.halisaha.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Şifre boş (null) bırakılırsa değiştirilmez. Rol bu istekle değiştirilemez. */
public record KullaniciGuncelleRequest(
        @NotBlank(message = "Kullanıcı adı zorunludur") String kullaniciAdi,
        @NotBlank(message = "Ad soyad zorunludur") String adSoyad,
        @Size(min = 6, message = "Şifre en az 6 karakter olmalıdır") String sifre
) {
}
