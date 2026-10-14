package com.msc.halisaha.service;

import com.msc.halisaha.TestOturum;
import com.msc.halisaha.common.AppZaman;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.dto.RezervasyonRequest;
import com.msc.halisaha.dto.RezervasyonResponse;
import com.msc.halisaha.entity.Isletme;
import com.msc.halisaha.entity.Saha;
import com.msc.halisaha.entity.SahaDurum;
import com.msc.halisaha.repository.IsletmeRepository;
import com.msc.halisaha.repository.KullaniciRepository;
import com.msc.halisaha.repository.SahaRepository;
import com.msc.halisaha.entity.Rol;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Rezervasyonlar tam saat ya da buçuklu saatte (12:00, 12:30 ...) başlayıp bitebilir. */
@SpringBootTest
@Transactional
class RezervasyonYarimSaatTest {

    @Autowired RezervasyonService rezervasyonService;
    @Autowired IsletmeRepository isletmeRepository;
    @Autowired KullaniciRepository kullaniciRepository;
    @Autowired SahaRepository sahaRepository;

    private Saha saha;
    private LocalDate gun;

    @BeforeEach
    void hazirla() {
        Isletme isletme = TestOturum.isletme(isletmeRepository, "A İşletmesi");
        TestOturum.girisYap(TestOturum.kullanici(kullaniciRepository, isletme, Rol.ADMIN));
        saha = sahaRepository.save(Saha.builder().isletme(isletme).ad("Saha 1").durum(SahaDurum.AKTIF)
                .saatlikUcret(new BigDecimal("1000")).build());
        gun = AppZaman.bugun().plusDays(5);
    }

    @AfterEach
    void temizle() {
        TestOturum.temizle();
    }

    private RezervasyonResponse olustur(LocalDateTime baslangic, LocalDateTime bitis) {
        return rezervasyonService.create(new RezervasyonRequest(saha.getId(), "Ali", baslangic, bitis,
                "05469764613", null, null, null, null));
    }

    private ApiException hata(Runnable is) {
        return assertThrows(ApiException.class, is::run);
    }

    @Test
    void buckluSaatteBaslayanRezervasyonKaydedilirFiyatSureyeGoreHesaplanir() {
        RezervasyonResponse bir = olustur(gun.atTime(12, 30), gun.atTime(13, 30));
        assertEquals(gun.atTime(12, 30), bir.baslangicTarih());
        assertEquals(0, new BigDecimal("1000").compareTo(bir.odenecekTutar()));

        // 1,5 saatlik rezervasyon: saatlik ücretin 1,5 katı
        RezervasyonResponse buçuk = olustur(gun.atTime(15, 0), gun.atTime(16, 30));
        assertEquals(0, new BigDecimal("1500").compareTo(buçuk.odenecekTutar()));
    }

    @Test
    void cakismaYarimSaatHassasiyetindeHesaplanir() {
        olustur(gun.atTime(12, 0), gun.atTime(13, 0));
        // 12:30-13:30, 12:00-13:00 ile çakışır
        assertEquals(409, hata(() -> olustur(gun.atTime(12, 30), gun.atTime(13, 30))).getStatus().value());
        // 13:00-14:00 ve 11:30-12:00 bitişik, çakışmaz
        olustur(gun.atTime(13, 0), gun.atTime(14, 0));
        olustur(gun.atTime(11, 0), gun.atTime(12, 0));
        // 14:30-15:30 serbest; sonrasında 15:00-16:00 çakışır
        olustur(gun.atTime(14, 30), gun.atTime(15, 30));
        assertEquals(409, hata(() -> olustur(gun.atTime(15, 0), gun.atTime(16, 0))).getStatus().value());
    }

    @Test
    void gecedenSonrakiBuckluSaatYazilir() {
        // 23:30 - 00:30 (ertesi gün)
        RezervasyonResponse r = olustur(gun.atTime(23, 30), gun.plusDays(1).atTime(0, 30));
        assertEquals(gun.plusDays(1).atTime(0, 30), r.bitisTarih());
        assertEquals(0, new BigDecimal("1000").compareTo(r.odenecekTutar()));
    }

    @Test
    void dakikaYalnizcaSifirYaDaOtuzOlabilir() {
        assertEquals(400, hata(() -> olustur(gun.atTime(12, 15), gun.atTime(13, 15))).getStatus().value());
        assertEquals(400, hata(() -> olustur(gun.atTime(12, 0), gun.atTime(13, 10))).getStatus().value());
        assertEquals(400, hata(() -> olustur(gun.atTime(12, 0, 30), gun.atTime(13, 0))).getStatus().value());
    }
}
