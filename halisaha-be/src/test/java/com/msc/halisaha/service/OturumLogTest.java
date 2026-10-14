package com.msc.halisaha.service;

import com.msc.halisaha.common.security.IstemciBilgisi;
import com.msc.halisaha.dto.LoginRequest;
import com.msc.halisaha.dto.RefreshRequest;
import com.msc.halisaha.dto.TokenResponse;
import com.msc.halisaha.entity.Isletme;
import com.msc.halisaha.entity.Kullanici;
import com.msc.halisaha.entity.KullaniciDurum;
import com.msc.halisaha.entity.OturumLog;
import com.msc.halisaha.entity.OturumOlay;
import com.msc.halisaha.entity.Rol;
import com.msc.halisaha.repository.IsletmeRepository;
import com.msc.halisaha.repository.KullaniciRepository;
import com.msc.halisaha.repository.OturumLogRepository;
import com.msc.halisaha.repository.RefreshTokenRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Giriş/çıkış kaydı ayrı bir işlemde (REQUIRES_NEW) yazıldığı için bu test, diğerleri gibi geri alınan bir işlemde çalışamaz:
 * veriyi gerçekten kaydeder ve sonunda kendi eklediklerini siler.
 */
@SpringBootTest
class OturumLogTest {

    @Autowired AuthService authService;
    @Autowired IsletmeRepository isletmeRepository;
    @Autowired KullaniciRepository kullaniciRepository;
    @Autowired OturumLogRepository oturumLogRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private Isletme isletme;
    private Kullanici kullanici;
    private String kullaniciAdi;

    @BeforeEach
    void hazirla() {
        isletme = isletmeRepository.save(Isletme.builder().ad("Log Test İşletmesi").build());
        kullaniciAdi = "log-" + UUID.randomUUID();
        kullanici = kullaniciRepository.save(Kullanici.builder().isletme(isletme).kullaniciAdi(kullaniciAdi)
                .sifre(passwordEncoder.encode("Sifre123!")).adSoyad("Log Kullanıcı").rol(Rol.PERSONEL).durum(KullaniciDurum.AKTIF).build());
    }

    @AfterEach
    void temizle() {
        oturumLogRepository.deleteAll(oturumLogRepository.findAll().stream()
                .filter(l -> kullaniciAdi.equals(l.getKullaniciAdi()) || kullaniciAdi.equals("?")).toList());
        refreshTokenRepository.findAll().stream().filter(t -> t.getKullanici().getId().equals(kullanici.getId()))
                .forEach(refreshTokenRepository::delete);
        kullaniciRepository.delete(kullanici);
        isletmeRepository.delete(isletme);
    }

    private List<OturumLog> kayitlar() {
        return oturumLogRepository.findAll().stream().filter(l -> kullaniciAdi.equals(l.getKullaniciAdi())).toList();
    }

    @Test
    void girisCikisVeBasarisizGirisKullaniciBazindaKaydedilir() {
        IstemciBilgisi bilgi = new IstemciBilgisi("10.0.0.7", "Test Tarayici");

        // Yanlış şifre: başarısız giriş kaydı, işlem hata fırlatsa da saklanır
        assertThrows(BadCredentialsException.class, () -> authService.login(new LoginRequest(kullaniciAdi, "yanlis"), bilgi));
        assertEquals(1, kayitlar().size());
        assertEquals(OturumOlay.BASARISIZ_GIRIS, kayitlar().get(0).getOlay());
        assertEquals(kullanici.getId(), kayitlar().get(0).getKullaniciId());
        assertEquals(isletme.getId(), kayitlar().get(0).getIsletmeId());

        TokenResponse token = authService.login(new LoginRequest(kullaniciAdi, "Sifre123!"), bilgi);
        assertEquals(2, kayitlar().size());
        OturumLog giris = kayitlar().stream().filter(l -> l.getOlay() == OturumOlay.GIRIS).findFirst().orElseThrow();
        assertEquals("10.0.0.7", giris.getIp());
        assertEquals("Test Tarayici", giris.getTarayici());

        authService.logout(new RefreshRequest(token.refreshToken()), bilgi);
        authService.logout(new RefreshRequest(token.refreshToken()), bilgi);    // aynı oturumun ikinci çıkışı yazılmaz
        assertEquals(3, kayitlar().size());
        assertEquals(1, kayitlar().stream().filter(l -> l.getOlay() == OturumOlay.CIKIS).count());
    }

    @Test
    void bilinmeyenKullaniciAdiylaDenemeIsletmesizKaydedilir() {
        String yok = "yok-" + UUID.randomUUID();
        assertThrows(BadCredentialsException.class, () -> authService.login(new LoginRequest(yok, "x"), IstemciBilgisi.BILINMIYOR));
        OturumLog log = oturumLogRepository.findAll().stream().filter(l -> yok.equals(l.getKullaniciAdi())).findFirst().orElseThrow();
        assertNull(log.getKullaniciId());
        assertNull(log.getIsletmeId());
        oturumLogRepository.delete(log);
    }
}
