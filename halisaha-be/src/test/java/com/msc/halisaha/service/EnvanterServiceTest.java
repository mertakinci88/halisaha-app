package com.msc.halisaha.service;

import com.msc.halisaha.TestOturum;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.dto.EnvanterRequest;
import com.msc.halisaha.dto.EnvanterResponse;
import com.msc.halisaha.entity.EnvanterTur;
import com.msc.halisaha.repository.EnvanterStokHareketRepository;
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

import static com.msc.halisaha.service.EnvanterTestKurulum.istek;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class EnvanterServiceTest {

    @Autowired EnvanterService envanterService;
    @Autowired EnvanterSatisService satisService;
    @Autowired EnvanterStokHareketRepository hareketRepository;
    @Autowired IsletmeRepository isletmeRepository;
    @Autowired KullaniciRepository kullaniciRepository;
    @Autowired OgrenciRepository ogrenciRepository;

    private EnvanterTestKurulum k;

    @BeforeEach
    void hazirla() {
        k = new EnvanterTestKurulum(isletmeRepository, kullaniciRepository, ogrenciRepository, envanterService);
    }

    @AfterEach
    void temizle() {
        TestOturum.temizle();
    }

    private ApiException hata(Runnable is) {
        return assertThrows(ApiException.class, is::run);
    }

    @Test
    void bedensizTurdeBedenStandartOlurBedenliTurdeGecerliBedenZorunludur() {
        TestOturum.girisYap(k.b);   // ikinci işletmede çakışma olmadan denenir
        EnvanterResponse tozluk = envanterService.create(istek(EnvanterTur.TOZLUK, "Medium", 3, "100"));
        assertEquals("STANDART", tozluk.beden());          // gönderilen beden yok sayılır
        assertEquals(400, hata(() -> envanterService.create(istek(EnvanterTur.YAZLIK_FORMA, null, 1, "450"))).getStatus().value());
        assertEquals(400, hata(() -> envanterService.create(istek(EnvanterTur.YAZLIK_FORMA, "STANDART", 1, "450"))).getStatus().value());
    }

    @Test
    void gecerliBedenlerKabulEdilirEskiBedenlerReddedilir() {
        for (String beden : List.of("140", "152", "164", "176", "Small", "Medium")) {      // 128 kurulumda var
            assertEquals(beden, envanterService.create(istek(EnvanterTur.YAZLIK_FORMA, beden, 1, "450")).beden());
        }
        for (String eski : List.of("156", "172", "Large")) {
            assertEquals(400, hata(() -> envanterService.create(istek(EnvanterTur.YAZLIK_FORMA, eski, 1, "450"))).getStatus().value());
        }
    }

    @Test
    void ayniEnvanterVeBedenIkinciKezKaydedilemez() {
        ApiException e = hata(() -> envanterService.create(istek(EnvanterTur.YAZLIK_FORMA, "128", 3, "450")));
        assertEquals(409, e.getStatus().value());
        assertTrue(e.getMessage().contains("zaten kayıtlı"));
    }

    @Test
    void listeTurAlfabetikVeBedenSirasiyla() {
        var liste = envanterService.list();
        assertEquals(List.of("Kışlık Eşofman", "Kışlık Eşofman", "Krampon Çantası", "Tozluk", "Yağmurluk", "Yağmurluk", "Yazlık Forma"),
                liste.stream().map(EnvanterResponse::turAd).toList());
        assertEquals(List.of("140", "Medium"), liste.stream().filter(e -> e.tur() == EnvanterTur.KISLIK_ESOFMAN).map(EnvanterResponse::beden).toList());
        assertEquals(List.of("164", "Small"), liste.stream().filter(e -> e.tur() == EnvanterTur.YAGMURLUK).map(EnvanterResponse::beden).toList());
    }

    @Test
    void ilkGirisVeAdetDegisimiStokHareketiOlusturur() {
        long onceki = hareketRepository.count();
        EnvanterResponse yeni = envanterService.create(istek(EnvanterTur.YAZLIK_FORMA, "Small", 10, "450"));
        assertEquals(onceki + 1, hareketRepository.count());       // ilk stok girişi
        envanterService.update(yeni.id(), new EnvanterRequest(EnvanterTur.YAZLIK_FORMA, "Small", 14, new BigDecimal("475"), 5));
        assertEquals(onceki + 2, hareketRepository.count());       // elle düzeltme
        envanterService.update(yeni.id(), new EnvanterRequest(EnvanterTur.YAZLIK_FORMA, "Small", 14, new BigDecimal("480"), 5));
        assertEquals(onceki + 2, hareketRepository.count());       // yalnızca fiyat: hareket yok
        assertEquals(0, new BigDecimal("480").compareTo(envanterService.getById(yeni.id()).satisFiyati()));
    }

    @Test
    void kullanilmayanEnvanterinTuruVeBedeniDegisebilirKullanilaninDegisemez() {
        EnvanterResponse medium = envanterService.create(istek(EnvanterTur.YAZLIK_FORMA, "Medium", 25, "450"));
        // Medium'u Small'a çevir (satışta kullanılmıyor)
        EnvanterResponse small = envanterService.update(medium.id(), new EnvanterRequest(EnvanterTur.YAZLIK_FORMA, "Small", 25, new BigDecimal("450"), 5));
        assertEquals("Small", small.beden());
        // Aynı envanter + beden başka kayıtla çakışırsa engellenir
        assertEquals(409, hata(() -> envanterService.update(small.id(), new EnvanterRequest(EnvanterTur.YAZLIK_FORMA, "128", 25, new BigDecimal("450"), 5))).getStatus().value());

        // Satışta kullanılan envanterin türü/bedeni değişemez, adet ve fiyat değişebilir; silinemez
        satisService.olustur(k.ogrenciA.getId(), EnvanterTestKurulum.set("140", "164", List.of(), List.of(EnvanterTur.TOZLUK), null));
        assertEquals(409, hata(() -> envanterService.update(k.forma128.id(), new EnvanterRequest(EnvanterTur.YAZLIK_FORMA, "152", 25, new BigDecimal("450"), 5))).getStatus().value());
        envanterService.update(k.forma128.id(), new EnvanterRequest(EnvanterTur.YAZLIK_FORMA, "128", 30, new BigDecimal("460"), 5));
        assertEquals(409, hata(() -> envanterService.delete(k.forma128.id())).getStatus().value());
        envanterService.delete(small.id());      // kullanılmayan silinebilir
    }

    @Test
    void baskaIsletmeEnvanteriniGoremezDegistiremez() {
        TestOturum.girisYap(k.b);
        assertTrue(envanterService.list().isEmpty());
        assertEquals(404, hata(() -> envanterService.getById(k.tozluk.id())).getStatus().value());
        assertEquals(404, hata(() -> envanterService.delete(k.tozluk.id())).getStatus().value());
        assertEquals(404, hata(() -> envanterService.update(k.tozluk.id(), istek(EnvanterTur.TOZLUK, null, 1, "100"))).getStatus().value());
        // Aynı tür ve beden başka işletmede serbestçe tanımlanabilir
        envanterService.create(istek(EnvanterTur.TOZLUK, null, 7, "120"));
    }
}
