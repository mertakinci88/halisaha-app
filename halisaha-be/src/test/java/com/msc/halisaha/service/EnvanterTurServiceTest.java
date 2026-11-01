package com.msc.halisaha.service;

import com.msc.halisaha.TestOturum;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.dto.EnvanterRequest;
import com.msc.halisaha.dto.EnvanterTurRequest;
import com.msc.halisaha.dto.EnvanterTurResponse;
import com.msc.halisaha.repository.IsletmeRepository;
import com.msc.halisaha.repository.KullaniciRepository;
import com.msc.halisaha.repository.OgrenciRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class EnvanterTurServiceTest {

    @Autowired EnvanterTurService turService;
    @Autowired EnvanterService envanterService;
    @Autowired IsletmeRepository isletmeRepository;
    @Autowired KullaniciRepository kullaniciRepository;
    @Autowired OgrenciRepository ogrenciRepository;

    private EnvanterTestKurulum k;

    @BeforeEach
    void hazirla() {
        k = new EnvanterTestKurulum(isletmeRepository, kullaniciRepository, ogrenciRepository, envanterService, turService);
    }

    @AfterEach
    void temizle() {
        TestOturum.temizle();
    }

    private int durum(Runnable is) {
        return assertThrows(ApiException.class, is::run).getStatus().value();
    }

    @Test
    void turOlusturulurBedenlerSiraylaTutulurTurkceAlfabetikListelenir() {
        EnvanterTurResponse tur = turService.create(new EnvanterTurRequest("Çorap", true, "çift", List.of("S", " M ", "L")));
        assertEquals(List.of("S", "M", "L"), tur.bedenler());
        assertFalse(tur.kullanimda());
        List<String> adlar = turService.list().stream().map(EnvanterTurResponse::ad).toList();
        assertTrue(adlar.indexOf("Çorap") < adlar.indexOf("Kaleci Forma") && adlar.indexOf("Yağmurluk") < adlar.indexOf("Yazlık Forma"));
    }

    @Test
    void gecersizTurIstekleriReddedilir() {
        assertEquals(409, durum(() -> turService.create(new EnvanterTurRequest("tozluk", false, "adet", null))));   // ad çakışması (büyük/küçük harf duyarsız)
        assertEquals(400, durum(() -> turService.create(new EnvanterTurRequest("Eldiven", true, "adet", List.of()))));
        assertEquals(400, durum(() -> turService.create(new EnvanterTurRequest("Eldiven", true, "adet", List.of("S", "s")))));
        assertEquals(400, durum(() -> turService.create(new EnvanterTurRequest("Eldiven", true, "adet", List.of("STANDART")))));
        assertEquals(400, durum(() -> turService.create(new EnvanterTurRequest("Eldiven", true, "adet", List.of("12345678901")))));
        assertTrue(turService.create(new EnvanterTurRequest("Eldiven", false, "adet", List.of("S"))).bedenler().isEmpty());   // bedensizde yok sayılır
    }

    @Test
    void yeniTureEnvanterEklenirKullanilanTurSilinemezBedeniKaldirilamaz() {
        EnvanterTurResponse tur = turService.create(new EnvanterTurRequest("Eldiven", true, "adet", List.of("S", "M")));
        var envanter = envanterService.create(new EnvanterRequest(tur.id(), "M", 5, new BigDecimal("100"), 2));
        assertEquals("Eldiven", envanter.turAd());

        assertEquals(400, durum(() -> envanterService.create(new EnvanterRequest(tur.id(), "L", 1, new BigDecimal("100"), 2))));   // L tanımlı değil
        assertTrue(turService.list().stream().filter(t -> t.id().equals(tur.id())).findFirst().orElseThrow().kullanimda());

        assertEquals(409, durum(() -> turService.delete(tur.id())));
        assertEquals(409, durum(() -> turService.update(tur.id(), new EnvanterTurRequest("Eldiven", true, "adet", List.of("S")))));      // M kullanımda
        assertEquals(409, durum(() -> turService.update(tur.id(), new EnvanterTurRequest("Eldiven", false, "adet", null))));             // bedensiz olamaz
        EnvanterTurResponse guncel = turService.update(tur.id(), new EnvanterTurRequest("Eldiven Seti", true, "çift", List.of("M", "L")));
        assertEquals(List.of("M", "L"), guncel.bedenler());
        assertEquals("çift", guncel.birim());

        envanterService.delete(envanter.id());
        turService.delete(tur.id());
    }

    @Test
    void baskaIsletmeninTuruneErisilemez() {
        EnvanterTurResponse tur = turService.create(new EnvanterTurRequest("Eldiven", false, "adet", null));
        TestOturum.girisYap(k.b);
        assertTrue(turService.list().isEmpty());
        assertEquals(404, durum(() -> turService.delete(tur.id())));
        assertEquals(404, durum(() -> turService.update(tur.id(), new EnvanterTurRequest("X", false, "adet", null))));
        assertEquals(404, durum(() -> envanterService.create(new EnvanterRequest(tur.id(), null, 1, new BigDecimal("10"), 1))));
        // Aynı ad başka işletmede serbest
        turService.create(new EnvanterTurRequest("Eldiven", false, "adet", null));
    }
}
