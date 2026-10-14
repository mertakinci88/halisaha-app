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
        LocalDateTime kayitTarih,
        Long isletmeId,
        String isletmeAd,
        boolean grupKisitli,
        Set<Long> grupIdleri
) {

    public static KullaniciResponse from(Kullanici kullanici) {
        return new KullaniciResponse(
                kullanici.getId(),
                kullanici.getKullaniciAdi(),
                kullanici.getAdSoyad(),
                kullanici.getRol(),
                kullanici.getDurum(),
                kullanici.getRol() == Rol.ADMIN ? Set.of(Modul.values())
                        : kullanici.getRol() == Rol.SUPER_ADMIN ? Set.of() : kullanici.getYetkiler(),
                kullanici.getKayitTarih(),
                kullanici.getIsletme() != null ? kullanici.getIsletme().getId() : null,
                kullanici.getIsletme() != null ? kullanici.getIsletme().getAd() : null,
                kullanici.getRol() == Rol.PERSONEL && kullanici.isGrupKisitli(),
                kullanici.getRol() == Rol.PERSONEL && kullanici.isGrupKisitli() ? Set.copyOf(kullanici.getIzinliGruplar()) : Set.of()
        );
    }
}
