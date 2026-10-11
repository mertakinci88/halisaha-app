package com.msc.halisaha.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** İşletme ve ilk yöneticisi (ADMIN) birlikte oluşturulur. */
public record IsletmeOlusturRequest(
        @NotBlank(message = "İşletme adı zorunludur") String ad,
        String yoneticiAdSoyad,
        String yoneticiTelNo,
        @NotNull(message = "Yönetici bilgileri zorunludur") @Valid Yonetici yonetici
) {

    public record Yonetici(
            @NotBlank(message = "Kullanıcı adı zorunludur") String kullaniciAdi,
            @NotBlank(message = "Şifre zorunludur") @Size(min = 6, message = "Şifre en az 6 karakter olmalıdır") String sifre,
            @NotBlank(message = "Ad soyad zorunludur") String adSoyad
    ) {
    }
}
