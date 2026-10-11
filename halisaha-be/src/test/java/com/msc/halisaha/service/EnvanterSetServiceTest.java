package com.msc.halisaha.service;

import com.msc.halisaha.TestOturum;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.dto.EnvanterSatisRequest;
import com.msc.halisaha.dto.EnvanterSatisResponse;
import com.msc.halisaha.dto.EnvanterSetRequest;
import com.msc.halisaha.dto.EnvanterSetResponse;
import com.msc.halisaha.entity.EnvanterTur;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Her test geri alınan bir transaction'da çalışır; veritabanında kalıcı veri bırakmaz. */
@SpringBootTest
@Transactional
class EnvanterSetServiceTest {

    @Autowired EnvanterSetService setService;
    @Autowired EnvanterSatisService satisService;
    @Autowired EnvanterService envanterService;
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

    private EnvanterSetResponse kucukSet(String ad) {
        return setService.create(new EnvanterSetRequest(ad, List.of(k.forma128.id(), k.esofman140.id(), k.tozluk.id())));
    }

    @Test
    void setOlusturulurToplamVeKaydedenGelir() {
        EnvanterSetResponse set = kucukSet("128 Beden Seti");
        assertEquals(3, set.kalemler().size());
        assertEquals(0, new BigDecimal("1200").compareTo(set.toplamTutar()));   // 450 + 600 + 150
        assertEquals(k.a.getAdSoyad(), set.kaydeden());
        assertNotNull(set.kayitTarihi());
    }

    @Test
    void ayniAdliSetOlusmaz() {
        kucukSet("Set A");
        ApiException e = assertThrows(ApiException.class, () -> kucukSet("set a"));
        assertEquals(409, e.getStatus().value());
    }

    @Test
    void ayniTurdenIkiKayitVeBosListeReddedilir() {
        assertThrows(ApiException.class, () -> setService.create(
                new EnvanterSetRequest("X", List.of(k.yagmurluk164.id(), k.yagmurlukSmall.id()))));
        assertThrows(ApiException.class, () -> setService.create(new EnvanterSetRequest("Y", List.of())));
        assertEquals(0, setService.list().size());
    }

    @Test
    void guncellenebilirVeBaskaIsletmeninSetineErisilemez() {
        EnvanterSetResponse set = kucukSet("Set A");
        EnvanterSetResponse yeni = setService.update(set.id(), new EnvanterSetRequest("Set B", List.of(k.krampon.id())));
        assertEquals("Set B", yeni.ad());
        assertEquals(1, yeni.kalemler().size());

        TestOturum.girisYap(k.b);
        assertThrows(ApiException.class, () -> setService.getById(set.id()));
        assertEquals(0, setService.list().size());
    }

    @Test
    void setteKullanilanEnvanterSilinemezVeKimligiDegisemez() {
        kucukSet("Set A");
        assertEquals(409, assertThrows(ApiException.class, () -> envanterService.delete(k.tozluk.id())).getStatus().value());
        assertEquals(409, assertThrows(ApiException.class, () -> envanterService.update(k.forma128.id(),
                EnvanterTestKurulum.istek(EnvanterTur.YAZLIK_FORMA, "Small", 25, "450"))).getStatus().value());
        // adet/fiyat güncellenebilir, set toplamı yeni fiyata göre hesaplanır
        envanterService.update(k.tozluk.id(), EnvanterTestKurulum.istek(EnvanterTur.TOZLUK, null, 40, "200"));
        assertEquals(0, new BigDecimal("1250").compareTo(setService.list().get(0).toplamTutar()));
    }

    @Test
    void setSilinebilirVeEnvanterlerSerbestKalir() {
        EnvanterSetResponse set = kucukSet("Set A");
        setService.delete(set.id());
        assertEquals(0, setService.list().size());
        envanterService.delete(k.tozluk.id());
    }

    @Test
    void setSeciliSatisSetAdiniTutarVeBedenDegisebilir() {
        EnvanterSetResponse set = kucukSet("128 Beden Seti");
        List<EnvanterSatisRequest.Kalem> kalemler = List.of(
                new EnvanterSatisRequest.Kalem(EnvanterTur.YAZLIK_FORMA, "128", true, false),
                new EnvanterSatisRequest.Kalem(EnvanterTur.KISLIK_ESOFMAN, "Medium", false, false),   // beden değişti
                new EnvanterSatisRequest.Kalem(EnvanterTur.TOZLUK, null, false, false));
        EnvanterSatisResponse satis = satisService.olustur(k.ogrenciA.getId(), new EnvanterSatisRequest(kalemler, null, set.id()));
        assertEquals(set.id(), satis.setId());
        assertEquals("128 Beden Seti", satis.setAd());
        assertEquals(3, satis.kalemler().size());

        // set silinse de satışta ad kalır
        setService.delete(set.id());
        EnvanterSatisResponse sonra = satisService.listele(k.ogrenciA.getId()).get(0);
        assertNull(sonra.setId());
        assertEquals("128 Beden Seti", sonra.setAd());
    }

    @Test
    void setTurleriyleUyusmayanSatisVeBilinmeyenSetReddedilir() {
        EnvanterSetResponse set = kucukSet("Set A");
        List<EnvanterSatisRequest.Kalem> eksik = List.of(new EnvanterSatisRequest.Kalem(EnvanterTur.TOZLUK, null, true, false));
        assertEquals(400, assertThrows(ApiException.class, () -> satisService.olustur(
                k.ogrenciA.getId(), new EnvanterSatisRequest(eksik, null, set.id()))).getStatus().value());
        assertEquals(404, assertThrows(ApiException.class, () -> satisService.olustur(
                k.ogrenciA.getId(), new EnvanterSatisRequest(eksik, null, 999999L))).getStatus().value());
        assertEquals(0, satisService.listele(k.ogrenciA.getId()).size());
    }

    @Test
    void setsizOzelSatisAdsizKalir() {
        EnvanterSatisResponse satis = satisService.olustur(k.ogrenciA.getId(), new EnvanterSatisRequest(
                List.of(new EnvanterSatisRequest.Kalem(EnvanterTur.TOZLUK, null, true, false)), null));
        assertNull(satis.setId());
        assertNull(satis.setAd());
    }
}
