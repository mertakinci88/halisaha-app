package com.msc.halisaha.service;

import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.dto.RezervasyonRequest;
import com.msc.halisaha.dto.RezervasyonResponse;
import com.msc.halisaha.dto.RezervasyonUrunRequest;
import com.msc.halisaha.dto.UrunResponse;
import com.msc.halisaha.entity.Saha;
import com.msc.halisaha.entity.SahaDurum;
import com.msc.halisaha.entity.Urun;
import com.msc.halisaha.repository.SahaRepository;
import com.msc.halisaha.repository.StokHareketRepository;
import com.msc.halisaha.repository.UrunRepository;
import com.msc.halisaha.TestOturum;
import com.msc.halisaha.entity.Isletme;
import com.msc.halisaha.entity.Rol;
import com.msc.halisaha.repository.IsletmeRepository;
import com.msc.halisaha.repository.KullaniciRepository;
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

/** Her test kendi transaction'ında çalışır ve geri alınır; veritabanında kalıcı veri bırakmaz. */
@SpringBootTest
@Transactional
class RezervasyonStokTest {

    @Autowired RezervasyonService rezervasyonService;
    @Autowired SahaRepository sahaRepository;
    @Autowired UrunRepository urunRepository;
    @Autowired StokHareketRepository stokHareketRepository;
    @Autowired UrunService urunService;

    @Autowired IsletmeRepository isletmeRepository;
    @Autowired KullaniciRepository kullaniciRepository;

    private Saha saha;
    private Urun cips;

    @BeforeEach
    void hazirla() {
        Isletme isletme = TestOturum.isletme(isletmeRepository, "Test İşletmesi");
        TestOturum.girisYap(TestOturum.kullanici(kullaniciRepository, isletme, Rol.ADMIN));
        saha = sahaRepository.save(Saha.builder().isletme(isletme).ad("Test Sahası").durum(SahaDurum.AKTIF)
                .saatlikUcret(new BigDecimal("1000.00")).build());
        cips = urunRepository.save(Urun.builder().isletme(isletme).ad("Test Cips").adet(5).fiyat(new BigDecimal("80.00")).build());
    }

    @AfterEach
    void temizle() {
        TestOturum.temizle();
    }

    private RezervasyonResponse rezervasyonAc(int gun) {
        LocalDateTime bas = LocalDateTime.of(2030, 1, gun, 14, 0);
        return rezervasyonService.create(new RezervasyonRequest(
                saha.getId(), "Test Müşteri", bas, bas.plusHours(1), "05551112233", null, null, null, null));
    }

    private int stok() {
        return urunRepository.mevcutAdet(cips.getId());
    }

    @Test
    void yeniRezervasyonaKayitTarihiYazilir() {
        RezervasyonResponse r = rezervasyonAc(9);
        org.junit.jupiter.api.Assertions.assertNotNull(r.kayitTarih());
        assertEquals("Test Kullanıcı", r.kaydedenAd());
        org.junit.jupiter.api.Assertions.assertTrue(
                java.time.Duration.between(r.kayitTarih(), com.msc.halisaha.common.AppZaman.simdi()).abs().toMinutes() < 1);
    }

    @Test
    void urunEklenincePaketStoktanDuser() {
        RezervasyonResponse r = rezervasyonAc(10);
        rezervasyonService.urunEkle(r.id(), new RezervasyonUrunRequest(cips.getId(), 2));
        assertEquals(3, stok());
    }

    @Test
    void yetersizStoktaEklemeReddedilirVeStokDegismez() {
        RezervasyonResponse r = rezervasyonAc(11);
        ApiException hata = assertThrows(ApiException.class,
                () -> rezervasyonService.urunEkle(r.id(), new RezervasyonUrunRequest(cips.getId(), 6)));
        assertEquals(409, hata.getStatus().value());
        assertEquals(5, stok());
    }

    @Test
    void sonUrunSatilirStokSifirOlurSonrasiReddedilir() {
        RezervasyonResponse r = rezervasyonAc(12);
        rezervasyonService.urunEkle(r.id(), new RezervasyonUrunRequest(cips.getId(), 5));
        assertEquals(0, stok());
        assertThrows(ApiException.class,
                () -> rezervasyonService.urunEkle(r.id(), new RezervasyonUrunRequest(cips.getId(), 1)));
        assertEquals(0, stok());
    }

    @Test
    void urunCikarilincaStogaDoner() {
        RezervasyonResponse r = rezervasyonAc(13);
        RezervasyonResponse eklendi = rezervasyonService.urunEkle(r.id(), new RezervasyonUrunRequest(cips.getId(), 1));
        assertEquals(4, stok());
        rezervasyonService.urunCikar(r.id(), eklendi.urunler().get(0).id());
        assertEquals(5, stok());
    }

    @Test
    void iptalEdilinceTumUrunlerStogaDonerVeTekrarIptalIkinciKezIadeEtmez() {
        RezervasyonResponse r = rezervasyonAc(14);
        rezervasyonService.urunEkle(r.id(), new RezervasyonUrunRequest(cips.getId(), 1));
        rezervasyonService.urunEkle(r.id(), new RezervasyonUrunRequest(cips.getId(), 2));
        assertEquals(2, stok());

        rezervasyonService.cancel(r.id());
        assertEquals(5, stok());

        rezervasyonService.cancel(r.id());
        assertEquals(5, stok());
    }

    @Test
    void iptalEdilenRezervasyondanUrunCikarilamazVeEklenemez() {
        RezervasyonResponse r = rezervasyonAc(15);
        RezervasyonResponse eklendi = rezervasyonService.urunEkle(r.id(), new RezervasyonUrunRequest(cips.getId(), 1));
        rezervasyonService.cancel(r.id());

        assertThrows(ApiException.class, () -> rezervasyonService.urunCikar(r.id(), eklendi.urunler().get(0).id()));
        assertThrows(ApiException.class, () -> rezervasyonService.urunEkle(r.id(), new RezervasyonUrunRequest(cips.getId(), 1)));
        assertEquals(5, stok());
    }

    @Test
    void rezervasyonSilinincePaketStogaDoner() {
        RezervasyonResponse r = rezervasyonAc(16);
        rezervasyonService.urunEkle(r.id(), new RezervasyonUrunRequest(cips.getId(), 3));
        assertEquals(2, stok());

        rezervasyonService.delete(r.id());
        assertEquals(5, stok());
    }

    @Test
    void stokDegisinceUrunListesiSirasiDegismezVeAlfabetiktir() {
        Urun ikinci = urunRepository.save(Urun.builder().isletme(saha.getIsletme()).ad("Test Su").adet(9).fiyat(new BigDecimal("20.00")).build());
        Urun ucuncu = urunRepository.save(Urun.builder().isletme(saha.getIsletme()).ad("Test Kola").adet(9).fiyat(new BigDecimal("60.00")).build());
        java.util.function.Supplier<java.util.List<Long>> siralama = () -> urunService.list().stream()
                .map(UrunResponse::id)
                .filter(id -> id.equals(cips.getId()) || id.equals(ikinci.getId()) || id.equals(ucuncu.getId()))
                .toList();
        // Alfabetik: Test Cips, Test Kola, Test Su
        var beklenen = java.util.List.of(cips.getId(), ucuncu.getId(), ikinci.getId());
        assertEquals(beklenen, siralama.get());

        // Ortadaki ürünü sat: satır güncellenir, ama liste sırası aynı kalmalı
        RezervasyonResponse r = rezervasyonAc(18);
        rezervasyonService.urunEkle(r.id(), new RezervasyonUrunRequest(ucuncu.getId(), 1));
        assertEquals(beklenen, siralama.get());
    }

    @Test
    void herDegisiklikHareketKaydiOlusturur() {
        long onceki = stokHareketRepository.count();
        RezervasyonResponse r = rezervasyonAc(17);
        RezervasyonResponse eklendi = rezervasyonService.urunEkle(r.id(), new RezervasyonUrunRequest(cips.getId(), 2));
        rezervasyonService.urunCikar(r.id(), eklendi.urunler().get(0).id());
        assertEquals(onceki + 2, stokHareketRepository.count());
    }
}
