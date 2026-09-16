package com.msc.halisaha.dto;

import com.msc.halisaha.entity.Kullanici;
import com.msc.halisaha.entity.KullaniciDurum;
import com.msc.halisaha.entity.Modul;
import com.msc.halisaha.entity.Rol;

import java.time.LocalDateTime;
import java.util.Set;

public record KullaniciResponse(
        Long id,
        String kullaniciAdi,
        String adSoyad,
        Rol rol,
        KullaniciDurum durum,
        Set<Modul> yetkiler,
        LocalDateTime kayitTarih
) {

    public static KullaniciResponse from(Kullanici kullanici) {
        return new KullaniciResponse(
                kullanici.getId(),
                kullanici.getKullaniciAdi(),
                kullanici.getAdSoyad(),
                kullanici.getRol(),
                kullanici.getDurum(),
                kullanici.getRol() == Rol.ADMIN ? Set.of(Modul.values()) : kullanici.getYetkiler(),
                kullanici.getKayitTarih()
        );
    }
}
