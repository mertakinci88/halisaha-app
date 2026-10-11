package com.msc.halisaha;

import com.msc.halisaha.common.security.UserPrincipal;
import com.msc.halisaha.entity.Isletme;
import com.msc.halisaha.entity.Kullanici;
import com.msc.halisaha.entity.KullaniciDurum;
import com.msc.halisaha.entity.Rol;
import com.msc.halisaha.repository.IsletmeRepository;
import com.msc.halisaha.repository.KullaniciRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/** Servis testlerinde bir işletme kullanıcısı olarak "giriş yapmış" gibi davranmak için. */
public final class TestOturum {

    private TestOturum() {
    }

    public static Isletme isletme(IsletmeRepository repo, String ad) {
        return repo.save(Isletme.builder().ad(ad).build());
    }

    public static Kullanici kullanici(KullaniciRepository repo, Isletme isletme, Rol rol) {
        return repo.save(Kullanici.builder()
                .isletme(isletme)
                .kullaniciAdi("test-" + UUID.randomUUID())
                .sifre("x")
                .adSoyad("Test Kullanıcı")
                .rol(rol)
                .durum(KullaniciDurum.AKTIF)
                .build());
    }

    public static void girisYap(Kullanici kullanici) {
        UserPrincipal principal = new UserPrincipal(kullanici);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    public static void temizle() {
        SecurityContextHolder.clearContext();
    }
}
