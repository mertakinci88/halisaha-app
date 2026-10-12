package com.msc.halisaha.service;

import com.msc.halisaha.TestOturum;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.dto.KullaniciResponse;
import com.msc.halisaha.dto.RolGuncelleRequest;
import com.msc.halisaha.entity.Isletme;
import com.msc.halisaha.entity.Kullanici;
import com.msc.halisaha.entity.Modul;
import com.msc.halisaha.entity.RefreshToken;
import com.msc.halisaha.entity.Rol;
import com.msc.halisaha.repository.IsletmeRepository;
import com.msc.halisaha.repository.KullaniciRepository;
import com.msc.halisaha.repository.RefreshTokenRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class KullaniciRolGuncelleTest {

    @Autowired KullaniciService kullaniciService;
    @Autowired IsletmeRepository isletmeRepository;
    @Autowired KullaniciRepository kullaniciRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired EntityManager em;

    private Kullanici admin;
    private Kullanici personel;
    private Kullanici baskaIsletmePersonel;
    private RefreshToken token;

    @BeforeEach
    void hazirla() {
        Isletme a = TestOturum.isletme(isletmeRepository, "Rol A");
        Isletme b = TestOturum.isletme(isletmeRepository, "Rol B");
        admin = TestOturum.kullanici(kullaniciRepository, a, Rol.ADMIN);
        personel = TestOturum.kullanici(kullaniciRepository, a, Rol.PERSONEL);
        personel.getYetkiler().add(Modul.OGRENCI);
        baskaIsletmePersonel = TestOturum.kullanici(kullaniciRepository, b, Rol.PERSONEL);
        token = refreshTokenRepository.save(RefreshToken.builder()
                .kullanici(personel).token("rt-" + System.nanoTime())
                .expiryDate(Instant.now().plusSeconds(3600)).revoked(false).build());
        TestOturum.girisYap(admin);
    }

    @AfterEach
    void temizle() {
        TestOturum.temizle();
    }

    private int durum(Runnable is) {
        return assertThrows(ApiException.class, is::run).getStatus().value();
    }

    @Test
    void personelAdminYapilirYetkilerTemizlenirVeOturumlarKapanir() {
        KullaniciResponse r = kullaniciService.rolGuncelle(personel.getId(), new RolGuncelleRequest(Rol.ADMIN));
        assertEquals(Rol.ADMIN, r.rol());
        em.flush();
        em.clear();
        Kullanici guncel = kullaniciRepository.findById(personel.getId()).orElseThrow();
        assertEquals(Rol.ADMIN, guncel.getRol());
        assertTrue(guncel.getYetkiler().isEmpty());
        assertTrue(refreshTokenRepository.findById(token.getId()).orElseThrow().isRevoked());
    }

    @Test
    void adminPersonelYapilabilir() {
        Kullanici digerAdmin = TestOturum.kullanici(kullaniciRepository, admin.getIsletme(), Rol.ADMIN);
        assertEquals(Rol.PERSONEL, kullaniciService.rolGuncelle(digerAdmin.getId(), new RolGuncelleRequest(Rol.PERSONEL)).rol());
    }

    @Test
    void kendiRolunuDegistiremez() {
        assertEquals(400, durum(() -> kullaniciService.rolGuncelle(admin.getId(), new RolGuncelleRequest(Rol.PERSONEL))));
    }

    @Test
    void superAdminRoluAtanamaz() {
        assertEquals(400, durum(() -> kullaniciService.rolGuncelle(personel.getId(), new RolGuncelleRequest(Rol.SUPER_ADMIN))));
    }

    @Test
    void baskaIsletmeninKullanicisi404Doner() {
        assertEquals(404, durum(() -> kullaniciService.rolGuncelle(baskaIsletmePersonel.getId(), new RolGuncelleRequest(Rol.ADMIN))));
    }
}
