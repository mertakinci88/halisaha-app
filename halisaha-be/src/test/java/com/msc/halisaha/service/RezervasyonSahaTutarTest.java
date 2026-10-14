package com.msc.halisaha.service;

import com.msc.halisaha.TestOturum;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.dto.RezervasyonRequest;
import com.msc.halisaha.dto.RezervasyonResponse;
import com.msc.halisaha.entity.Isletme;
import com.msc.halisaha.entity.Kullanici;
import com.msc.halisaha.entity.OdemeYontemi;
import com.msc.halisaha.entity.Rol;
import com.msc.halisaha.entity.Saha;
import com.msc.halisaha.entity.SahaDurum;
import com.msc.halisaha.repository.IsletmeRepository;
import com.msc.halisaha.repository.KullaniciRepository;
import com.msc.halisaha.repository.SahaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class RezervasyonSahaTutarTest {

    @Autowired RezervasyonService rezervasyonService;
    @Autowired SahaRepository sahaRepository;
    @Autowired IsletmeRepository isletmeRepository;
    @Autowired KullaniciRepository kullaniciRepository;

    private Saha saha;

    @BeforeEach
    void hazirla() {
        Isletme isletme = TestOturum.isletme(isletmeRepository, "Tutar A");
        Kullanici admin = TestOturum.kullanici(kullaniciRepository, isletme, Rol.ADMIN);
        saha = sahaRepository.save(Saha.builder().isletme(isletme).ad("Saha").durum(SahaDurum.AKTIF)
                .saatlikUcret(new BigDecimal("1000.00")).build());
        TestOturum.girisYap(admin);
    }

    @AfterEach
    void temizle() {
        TestOturum.temizle();
    }

    private RezervasyonResponse olustur(int gun, int saat, BigDecimal sahaTutar, BigDecimal kapora, Boolean tamOdeme) {
        LocalDateTime bas = LocalDateTime.of(2030, 3, gun, saat, 0);
        return rezervasyonService.create(new RezervasyonRequest(saha.getId(), "Müşteri", bas, bas.plusHours(2),
                "05551112233", kapora != null || Boolean.TRUE.equals(tamOdeme) ? OdemeYontemi.NAKIT : null,
                kapora, null, tamOdeme, sahaTutar));
    }

    @Test
    void tutarVerilmezseSahaUcretiKullanilir() {
        RezervasyonResponse r = olustur(1, 14, null, null, null);
        assertEquals(0, new BigDecimal("2000.00").compareTo(r.odenecekTutar()));
    }

    @Test
    void elleGirilenTutarOdenecekTutarOlur() {
        RezervasyonResponse r = olustur(2, 14, new BigDecimal("1500"), null, null);
        assertEquals(0, new BigDecimal("1500.00").compareTo(r.odenecekTutar()));
        assertEquals(0, new BigDecimal("1500.00").compareTo(r.sahaTutar()));
        assertEquals(0, new BigDecimal("1500.00").compareTo(r.kalanTutar()));
    }

    @Test
    void tamOdemeElleGirilenTutarKadarAlinir() {
        RezervasyonResponse r = olustur(3, 14, new BigDecimal("1750.50"), null, true);
        assertEquals(0, new BigDecimal("1750.50").compareTo(r.odenenTutar()));
        assertEquals(0, BigDecimal.ZERO.compareTo(r.kalanTutar()));
    }

    @Test
    void kaporaElleGirilenTutariAsamaz() {
        ApiException e = assertThrows(ApiException.class,
                () -> olustur(4, 14, new BigDecimal("500"), new BigDecimal("600"), null));
        assertEquals(400, e.getStatus().value());
    }
}
