package com.msc.halisaha.service;

import com.msc.halisaha.TestOturum;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.dto.EnvanterSatisRequest;
import com.msc.halisaha.dto.EnvanterSatisResponse;
import com.msc.halisaha.dto.EnvanterSetRequest;
import com.msc.halisaha.dto.EnvanterSetResponse;
import com.msc.halisaha.dto.EnvanterResponse;
import com.msc.halisaha.entity.EnvanterTur;
import com.msc.halisaha.repository.EnvanterRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    @Autowired EnvanterRepository envanterRepository;

    // 152 bedeninde tüm bedenli türler tanımlı (set kurulabilen beden)
    private EnvanterResponse kaleci152;
    private EnvanterResponse esofman152;
    private EnvanterResponse yagmurluk152;
    private EnvanterResponse forma152;

    private EnvanterTestKurulum k;

    @BeforeEach
    void hazirla() {
        k = new EnvanterTestKurulum(isletmeRepository, kullaniciRepository, ogrenciRepository, envanterService);
        kaleci152 = envanterService.create(EnvanterTestKurulum.istek(EnvanterTur.KALECI_FORMA, "152", 6, "700"));
        esofman152 = envanterService.create(EnvanterTestKurulum.istek(EnvanterTur.KISLIK_ESOFMAN, "152", 6, "600"));
        yagmurluk152 = envanterService.create(EnvanterTestKurulum.istek(EnvanterTur.YAGMURLUK, "152", 6, "500"));
        forma152 = envanterService.create(EnvanterTestKurulum.istek(EnvanterTur.YAZLIK_FORMA, "152", 6, "450"));
    }

    @AfterEach
    void temizle() {
        TestOturum.temizle();
    }

    /** 152 bedeninin dört bedenli ürünü + istenen ekler. */
    private List<Long> tum152(Long... ekler) {
        List<Long> idler = new java.util.ArrayList<>(List.of(kaleci152.id(), esofman152.id(), yagmurluk152.id(), forma152.id()));
        idler.addAll(List.of(ekler));
        return idler;
    }

    private EnvanterSetResponse set152(String ad, Long... ekler) {
        return setService.create(new EnvanterSetRequest(ad, "152", new BigDecimal("1500"), tum152(ekler)));
    }

    private ApiException olusmaz(String beden, List<Long> idler) {
        return assertThrows(ApiException.class,
                () -> setService.create(new EnvanterSetRequest("X", beden, new BigDecimal("1000"), idler)));
    }

    @Test
    void setOlusurFiyatSetteDururSecilmeyenUrunGirmez() {
        EnvanterSetResponse set = set152("152 Seti");
        assertEquals("152", set.beden());
        assertEquals(0, new BigDecimal("1500").compareTo(set.tutar()));
        assertEquals(4, set.kalemler().size());
        assertEquals(k.a.getAdSoyad(), set.kaydeden());
        assertNotNull(set.kayitTarihi());

        // yalnızca seçilenler girer; bedensiz ürün de seçilebilir
        EnvanterSetResponse secili = setService.create(new EnvanterSetRequest("Küçük", "152", new BigDecimal("900"),
                List.of(forma152.id(), k.tozluk.id())));
        assertEquals(2, secili.kalemler().size());
        assertEquals(6, set152("Tam", k.tozluk.id(), k.krampon.id()).kalemler().size());
    }

    @Test
    void stoksuzVeyaBaskaBedenliVeyaTekrarliSecimSetiOlusturmaz() {
        // stoğu biten (eşofman Medium 0 adet)
        ApiException stoksuz = olusmaz("Medium", List.of(k.esofmanMedium.id()));
        assertEquals(400, stoksuz.getStatus().value());
        assertTrue(stoksuz.getMessage().contains("Stoğu biten") && stoksuz.getMessage().contains("Kışlık Eşofman"));

        // set bedeninden farklı bedenli ürün
        assertEquals(400, olusmaz("152", List.of(forma152.id(), k.esofman140.id())).getStatus().value());
        // aynı türden iki kayıt, boş liste, geçersiz beden
        assertEquals(400, olusmaz("Small", List.of(k.yagmurlukSmall.id(), k.yagmurluk164.id())).getStatus().value());
        assertEquals(400, olusmaz("152", List.of()).getStatus().value());
        assertEquals(400, olusmaz("99", List.of(forma152.id())).getStatus().value());
        assertEquals(400, olusmaz("STANDART", List.of(k.tozluk.id())).getStatus().value());
        assertEquals(0, setService.list().size());
    }

    @Test
    void ayniAdliSetOlusmaz() {
        set152("Set A");
        ApiException e = assertThrows(ApiException.class, () -> set152("set a"));
        assertEquals(409, e.getStatus().value());
    }

    @Test
    void guncellenebilirVeBaskaIsletmeninSetineErisilemez() {
        EnvanterSetResponse set = set152("Set A");
        EnvanterSetResponse yeni = setService.update(set.id(),
                new EnvanterSetRequest("Set B", "152", new BigDecimal("1800"), tum152(k.krampon.id())));
        assertEquals("Set B", yeni.ad());
        assertEquals(0, new BigDecimal("1800").compareTo(yeni.tutar()));
        assertEquals(5, yeni.kalemler().size());

        TestOturum.girisYap(k.b);
        assertThrows(ApiException.class, () -> setService.getById(set.id()));
        assertEquals(0, setService.list().size());
    }

    @Test
    void setteKullanilanEnvanterSilinemezVeKimligiDegisemez() {
        set152("Set A", k.tozluk.id());
        assertEquals(409, assertThrows(ApiException.class, () -> envanterService.delete(k.tozluk.id())).getStatus().value());
        assertEquals(409, assertThrows(ApiException.class, () -> envanterService.update(forma152.id(),
                EnvanterTestKurulum.istek(EnvanterTur.YAZLIK_FORMA, "Small", 25, "450"))).getStatus().value());
    }

    @Test
    void setSilinebilirVeEnvanterlerSerbestKalir() {
        EnvanterSetResponse set = set152("Set A", k.tozluk.id());
        setService.delete(set.id());
        assertEquals(0, setService.list().size());
        envanterService.delete(k.tozluk.id());
    }

    private List<EnvanterSatisRequest.Kalem> setKalemleri(boolean odendi, boolean teslim) {
        return List.of(
                new EnvanterSatisRequest.Kalem(EnvanterTur.KALECI_FORMA, "152", odendi, teslim),
                new EnvanterSatisRequest.Kalem(EnvanterTur.KISLIK_ESOFMAN, "152", odendi, teslim),
                new EnvanterSatisRequest.Kalem(EnvanterTur.YAGMURLUK, "152", odendi, teslim),
                new EnvanterSatisRequest.Kalem(EnvanterTur.YAZLIK_FORMA, "152", odendi, teslim));
    }

    @Test
    void setSatisiTutariSettenGelirKalemFiyatiYoktur() {
        EnvanterSetResponse set = set152("152 Seti");
        EnvanterSatisResponse satis = satisService.olustur(k.ogrenciA.getId(),
                new EnvanterSatisRequest(setKalemleri(false, false), new BigDecimal("500"), set.id()));
        assertTrue(satis.sabitTutar());
        assertEquals("152 Seti", satis.setAd());
        assertEquals(0, new BigDecimal("1500").compareTo(satis.setTutari()));
        assertEquals(0, new BigDecimal("1000").compareTo(satis.kalanTutar()));
        assertTrue(satis.kalemler().stream().allMatch(kalem -> kalem.birimFiyat().signum() == 0 && !kalem.odendi()));

        // set tutarı sonradan değişse de geçmiş satış değişmez
        setService.update(set.id(), new EnvanterSetRequest("152 Seti", "152", new BigDecimal("2000"), tum152()));
        assertEquals(0, new BigDecimal("1500").compareTo(satisService.listele(k.ogrenciA.getId()).get(0).setTutari()));
    }

    @Test
    void tamOdemeKalemleriOdendiYaparKismiOdemeYapmaz() {
        EnvanterSetResponse set = set152("152 Seti");
        EnvanterSatisResponse satis = satisService.olustur(k.ogrenciA.getId(),
                new EnvanterSatisRequest(setKalemleri(false, false), new BigDecimal("1500"), set.id()));
        assertTrue(satis.kalemler().stream().allMatch(EnvanterSatisResponse.KalemResponse::odendi));
        assertEquals(0, satis.kalanTutar().signum());
    }

    @Test
    void setSatisiGuncellenirkenOdenenVeKalemDurumlariDegisirTutarSabitKalir() {
        EnvanterSetResponse set = set152("152 Seti");
        EnvanterSatisResponse satis = satisService.olustur(k.ogrenciA.getId(),
                new EnvanterSatisRequest(setKalemleri(false, false), new BigDecimal("500"), set.id()));

        List<EnvanterSatisRequest.Kalem> kalemler = List.of(
                new EnvanterSatisRequest.Kalem(EnvanterTur.KALECI_FORMA, "152", true, true),
                new EnvanterSatisRequest.Kalem(EnvanterTur.KISLIK_ESOFMAN, "152", false, false),
                new EnvanterSatisRequest.Kalem(EnvanterTur.YAGMURLUK, "152", false, false),
                new EnvanterSatisRequest.Kalem(EnvanterTur.YAZLIK_FORMA, "152", false, false));
        EnvanterSatisResponse guncel = satisService.guncelle(k.ogrenciA.getId(), satis.id(),
                new EnvanterSatisRequest(kalemler, new BigDecimal("900")));
        assertEquals(0, new BigDecimal("1500").compareTo(guncel.setTutari()));
        assertEquals(0, new BigDecimal("600").compareTo(guncel.kalanTutar()));
        assertEquals(1, guncel.kalemler().stream().filter(EnvanterSatisResponse.KalemResponse::teslimEdildi).count());
        assertEquals(5, envanterRepository.mevcutAdet(kaleci152.id()));   // teslimle stok 6 → 5

        // set satışında envanter eklenip çıkarılamaz
        List<EnvanterSatisRequest.Kalem> eksik = kalemler.subList(0, 3);
        assertEquals(400, assertThrows(ApiException.class, () -> satisService.guncelle(
                k.ogrenciA.getId(), satis.id(), new EnvanterSatisRequest(eksik, new BigDecimal("900")))).getStatus().value());
    }

    @Test
    void setTurleriyleUyusmayanSatisVeBilinmeyenSetReddedilir() {
        EnvanterSetResponse set = set152("Set A");
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
