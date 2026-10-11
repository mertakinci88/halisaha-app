package com.msc.halisaha.common.security;

import com.msc.halisaha.entity.IsletmeDurum;
import com.msc.halisaha.entity.Kullanici;
import com.msc.halisaha.entity.KullaniciDurum;
import com.msc.halisaha.entity.Modul;
import com.msc.halisaha.entity.Rol;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Getter
public class UserPrincipal implements UserDetails {

    private final Kullanici kullanici;

    public UserPrincipal(Kullanici kullanici) {
        this.kullanici = kullanici;
    }

    public Long getId() {
        return kullanici.getId();
    }

    /** SUPER_ADMIN için null. */
    public Long getIsletmeId() {
        return kullanici.getIsletme() != null ? kullanici.getIsletme().getId() : null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (kullanici.getRol() == Rol.SUPER_ADMIN) {
            return List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        }
        Set<Modul> etkinYetkiler = kullanici.getRol() == Rol.ADMIN
                ? Set.of(Modul.values())
                : kullanici.getYetkiler();

        return Stream.concat(
                        Stream.of("ROLE_" + kullanici.getRol().name()),
                        etkinYetkiler.stream().map(modul -> "MODUL_" + modul.name()))
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    @Override
    public String getPassword() {
        return kullanici.getSifre();
    }

    @Override
    public String getUsername() {
        return kullanici.getKullaniciAdi();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return aktif();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return aktif();
    }

    /** Kullanıcı aktif olmalı; bağlı olduğu işletme de pasifse giriş yapılamaz. */
    private boolean aktif() {
        return kullanici.getDurum() == KullaniciDurum.AKTIF
                && (kullanici.getIsletme() == null || kullanici.getIsletme().getDurum() == IsletmeDurum.AKTIF);
    }
}
