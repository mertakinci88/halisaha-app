package com.msc.halisaha.service;

import com.msc.halisaha.TestOturum;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.dto.SifreDegistirRequest;
import com.msc.halisaha.entity.Isletme;
import com.msc.halisaha.entity.Kullanici;
import com.msc.halisaha.entity.RefreshToken;
import com.msc.halisaha.entity.Rol;
import com.msc.halisaha.repository.IsletmeRepository;
import com.msc.halisaha.repository.KullaniciRepository;
import com.msc.halisaha.repository.RefreshTokenRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class AuthServiceSifreDegistirTest {

    @Autowired AuthService authService;
    @Autowired IsletmeRepository isletmeRepository;
    @Autowired KullaniciRepository kullaniciRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired EntityManager em;

    private Kullanici kullanici;
    private RefreshToken token;

    @BeforeEach
    void hazirla() {
        Isletme isletme = TestOturum.isletme(isletmeRepository, "Şifre İşletmesi");
        kullanici = TestOturum.kullanici(kullaniciRepository, isletme, Rol.PERSONEL);
        kullanici.setSifre(passwordEncoder.encode("eski123"));
        token = refreshTokenRepository.save(RefreshToken.builder()
                .kullanici(kullanici)
                .token("rt-" + System.nanoTime())
                .expiryDate(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build());
    }

    @Test
    void dogruEskiSifreyleSifreDegisirVeOturumlarKapanir() {
        authService.sifreDegistir(kullanici.getId(), new SifreDegistirRequest("eski123", "yeni456"));

        em.flush();
        em.clear();
        Kullanici guncel = kullaniciRepository.findById(kullanici.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("yeni456", guncel.getSifre()));
        assertFalse(passwordEncoder.matches("eski123", guncel.getSifre()));
        assertTrue(refreshTokenRepository.findById(token.getId()).orElseThrow().isRevoked());
    }

    @Test
    void yanlisEskiSifre400DonerVeDegisiklikYapilmaz() {
        ApiException e = assertThrows(ApiException.class,
                () -> authService.sifreDegistir(kullanici.getId(), new SifreDegistirRequest("yanlis", "yeni456")));
        assertEquals(400, e.getStatus().value());

        em.flush();
        em.clear();
        assertTrue(passwordEncoder.matches("eski123", kullaniciRepository.findById(kullanici.getId()).orElseThrow().getSifre()));
        assertFalse(refreshTokenRepository.findById(token.getId()).orElseThrow().isRevoked());
    }

    @Test
    void yeniSifreEskisiyleAyniOlamaz() {
        ApiException e = assertThrows(ApiException.class,
                () -> authService.sifreDegistir(kullanici.getId(), new SifreDegistirRequest("eski123", "eski123")));
        assertEquals(400, e.getStatus().value());
        assertFalse(refreshTokenRepository.findById(token.getId()).orElseThrow().isRevoked());
    }
}
