package com.msc.halisaha.service;

import com.msc.halisaha.TestOturum;
import com.msc.halisaha.common.AppZaman;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.dto.AbonelikOdemeRequest;
import com.msc.halisaha.dto.AbonelikRequest;
import com.msc.halisaha.dto.EnvanterSatisResponse;
import com.msc.halisaha.dto.IadeRequest;
import com.msc.halisaha.dto.OgrenciOdemeAyRequest;
import com.msc.halisaha.dto.OgrenciOdemeKaydetRequest;
import com.msc.halisaha.dto.RezervasyonRequest;
import com.msc.halisaha.dto.RezervasyonResponse;
import com.msc.halisaha.entity.OdemeYontemi;
import com.msc.halisaha.entity.Saha;
import com.msc.halisaha.entity.SahaDurum;
import com.msc.halisaha.repository.EnvanterRepository;
import com.msc.halisaha.repository.IadeRepository;
import com.msc.halisaha.repository.IsletmeRepository;
import com.msc.halisaha.repository.KullaniciRepository;
import com.msc.halisaha.repository.OgrenciRepository;
import com.msc.halisaha.repository.SahaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.msc.halisaha.service.EnvanterTestKurulum.set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class IadeServiceTest {

    @Autowired IadeService iadeService;
    @Autowired IadeRepository iadeRepository;
    @Autowired RezervasyonService rezervasyonService;
    @Autowired OgrenciOdemeService ogrenciOdemeService;
    @Autowired EnvanterSatisService envanterSatisService;
    @Autowired EnvanterService envanterService;
    @Autowired AbonelikService abonelikService;
    @Autowired FinansRaporService finansRaporService;
    @Autowired IsletmeRepository isletmeRepository;
    @Autowired KullaniciRepository kullaniciRepository;
    @Autowired OgrenciRepository ogrenciRepository;
    @Autowired SahaRepository sahaRepository;
    @Autowired EnvanterRepository envanterRepository;

    private EnvanterTestKurulum k;
    private Saha saha;

    @BeforeEach
    void hazirla() {
        k = new EnvanterTestKurulum(isletmeRepository, kullaniciRepository, ogrenciRepository, envanterService);
        saha = sahaRepository.save(Saha.builder().isletme(k.a.getIsletme()).ad("Saha 1").durum(SahaDurum.AKTIF)
                .saatlikUcret(new BigDecimal("1000")).build());
    }

    @AfterEach
    void temizle() {
        TestOturum.temizle();
    }

    private ApiException hata(Runnable is) {
        return assertThrows(ApiException.class, is::run);
    }

    private IadeRequest rezIade(Long id, String tutar) {
        return new IadeRequest(id, null, null, null, new BigDecimal(tutar), OdemeYontemi.NAKIT, null, null, "Müşteri vazgeçti");
    }

    private RezervasyonResponse kaporaliRezervasyon() {
        LocalDateTime baslangic = AppZaman.bugun().plusDays(2).atTime(19, 0);
        return rezervasyonService.create(new RezervasyonRequest(saha.getId(), "Ali", baslangic, baslangic.plusHours(1),
                "05469764613", OdemeYontemi.NAKIT, new BigDecimal("1000"), null, false));
    }

    @Test
    void rezervasyonIadesiAlinanOdemeyiAsamazOdemeDurumuDegismez() {
        RezervasyonResponse rez = kaporaliRezervasyon();
        var iade = iadeService.yap(rezIade(rez.id(), "400"));
        assertEquals(0, new BigDecimal("400").compareTo(iade.tutar()));
        assertEquals("Müşteri vazgeçti", iade.neden());
        assertEquals(400, hata(() -> iadeService.yap(rezIade(rez.id(), "600.01"))).getStatus().value());
        iadeService.yap(rezIade(rez.id(), "600"));
        assertEquals(400, hata(() -> iadeService.yap(rezIade(rez.id(), "0.01"))).getStatus().value());   // tamamı iade edildi

        assertEquals(2, iadeService.liste(rez.id(), null, null, null).size());
        // Ödeme kaydı ve durumu değişmez
        assertEquals(0, new BigDecimal("1000").compareTo(rezervasyonService.getById(rez.id()).odenenTutar()));
    }

    @Test
    void iadeDogrulamalari() {
        RezervasyonResponse rez = kaporaliRezervasyon();
        assertEquals(400, hata(() -> iadeService.yap(new IadeRequest(rez.id(), null, null, null, new BigDecimal("100"),
                OdemeYontemi.HAVALE_EFT, null, null, null))).getStatus().value());                  // hesap zorunlu
        assertEquals(400, hata(() -> iadeService.yap(new IadeRequest(rez.id(), null, null, null, new BigDecimal("100"),
                OdemeYontemi.NAKIT, null, AppZaman.bugun().plusDays(1), null))).getStatus().value());   // ileri tarih
        assertEquals(400, hata(() -> iadeService.yap(new IadeRequest(null, null, null, null, new BigDecimal("100"),
                OdemeYontemi.NAKIT, null, null, null))).getStatus().value());                      // kaynak yok
        assertEquals(400, hata(() -> iadeService.yap(new IadeRequest(rez.id(), 1L, null, null, new BigDecimal("100"),
                OdemeYontemi.NAKIT, null, null, null))).getStatus().value());                      // birden fazla kaynak
        assertEquals(400, hata(() -> iadeService.liste(null, null, null, null)).getStatus().value());
    }

    @Test
    void ogrenciOdemesiIadesiOdemeyiIadeninAltinaDusurmeyeIzinVermez() {
        int yil = 2030;
        var yanit = ogrenciOdemeService.kaydet(k.ogrenciA.getId(), yil, new OgrenciOdemeKaydetRequest(List.of(
                new OgrenciOdemeAyRequest(1, new BigDecimal("500"), OdemeYontemi.NAKIT, null, LocalDate.of(yil, 1, 5), null))));
        Long odemeId = yanit.aylar().get(0).id();
        iadeService.yap(new IadeRequest(null, odemeId, null, null, new BigDecimal("200"), OdemeYontemi.KART, null, null, null));
        assertEquals(400, hata(() -> iadeService.yap(new IadeRequest(null, odemeId, null, null, new BigDecimal("300.01"),
                OdemeYontemi.NAKIT, null, null, null))).getStatus().value());

        // Ödeme iade tutarının altına düşürülemez ve silinemez; üstüne düşürülebilir
        assertEquals(400, hata(() -> ogrenciOdemeService.kaydet(k.ogrenciA.getId(), yil, new OgrenciOdemeKaydetRequest(List.of(
                new OgrenciOdemeAyRequest(1, new BigDecimal("100"), OdemeYontemi.NAKIT, null, LocalDate.of(yil, 1, 5), null))))).getStatus().value());
        assertEquals(400, hata(() -> ogrenciOdemeService.kaydet(k.ogrenciA.getId(), yil, new OgrenciOdemeKaydetRequest(List.of(
                new OgrenciOdemeAyRequest(1, BigDecimal.ZERO, null, null, null, null))))).getStatus().value());
        ogrenciOdemeService.kaydet(k.ogrenciA.getId(), yil, new OgrenciOdemeKaydetRequest(List.of(
                new OgrenciOdemeAyRequest(1, new BigDecimal("250"), OdemeYontemi.NAKIT, null, LocalDate.of(yil, 1, 5), null))));
    }

    @Test
    void envanterSatisiIadesiOdemeAzaltmayiVeSatisSilmeyiEngeller() {
        var istek = set("140", "164", List.of(), List.of(com.msc.halisaha.entity.EnvanterTur.TOZLUK), "450");
        EnvanterSatisResponse satis = envanterSatisService.olustur(k.ogrenciA.getId(), istek);
        iadeService.yap(new IadeRequest(null, null, satis.id(), null, new BigDecimal("100"), OdemeYontemi.NAKIT, null, null, null));

        assertEquals(400, hata(() -> envanterSatisService.guncelle(k.ogrenciA.getId(), satis.id(),
                new com.msc.halisaha.dto.EnvanterSatisRequest(istek.kalemler(), new BigDecimal("99"), null, null, null))).getStatus().value());
        assertEquals(409, hata(() -> envanterSatisService.sil(k.ogrenciA.getId(), satis.id())).getStatus().value());
        assertEquals(1, iadeService.liste(null, null, satis.id(), null).size());
    }

    @Test
    void abonelikIadesiVeOdemeSilmeKorumasi() {
        LocalDate baslangic = AppZaman.bugun().plusDays(3);
        var abonelik = abonelikService.olustur(new AbonelikRequest(saha.getId(), "Ali", "05469764613",
                baslangic.getDayOfWeek().getValue(), 19, 1, baslangic, new BigDecimal("10000"), new BigDecimal("4000"),
                OdemeYontemi.NAKIT, null, null, null));
        var iki = abonelikService.odemeEkle(abonelik.id(), new AbonelikOdemeRequest(new BigDecimal("2000"), OdemeYontemi.KART, null, null));
        iadeService.yap(new IadeRequest(null, null, null, abonelik.id(), new BigDecimal("3000"), OdemeYontemi.NAKIT, null, null, null));

        // 6000 ödendi, 3000 iade: ikinci ödeme (2000) silinirse 4000 kalır -> iadenin üstünde, olur; ilki silinirse 2000 < 3000 olur
        assertEquals(400, hata(() -> abonelikService.odemeSil(abonelik.id(), iki.odemeler().get(0).id())).getStatus().value());
        assertEquals(1, abonelikService.odemeSil(abonelik.id(), iki.odemeler().get(1).id()).odemeler().size());
    }

    @Test
    void iadeSilinirVeBaskaIsletmeErisemez() {
        RezervasyonResponse rez = kaporaliRezervasyon();
        var iade = iadeService.yap(rezIade(rez.id(), "300"));

        TestOturum.girisYap(k.b);
        assertEquals(404, hata(() -> iadeService.liste(rez.id(), null, null, null)).getStatus().value());
        assertEquals(404, hata(() -> iadeService.yap(rezIade(rez.id(), "100"))).getStatus().value());
        assertEquals(404, hata(() -> iadeService.sil(iade.id())).getStatus().value());

        TestOturum.girisYap(k.a);
        iadeService.sil(iade.id());
        assertEquals(0, iadeService.liste(rez.id(), null, null, null).size());
        iadeService.yap(rezIade(rez.id(), "1000"));    // silinince iade edilebilir tutar geri döner
    }

    @Test
    void finansRaporundaIadeYapildigiGunTahsilattanDusulur() {
        RezervasyonResponse rez = kaporaliRezervasyon();
        LocalDate bugun = AppZaman.bugun();
        var once = finansRaporService.getir(bugun, bugun);

        iadeService.yap(rezIade(rez.id(), "400"));
        iadeRepository.flush();
        var sonra = finansRaporService.getir(bugun, bugun);

        assertEquals(0, once.iade().add(new BigDecimal("400")).compareTo(sonra.iade()));
        assertEquals(0, once.tahsilat().rezervasyon().subtract(new BigDecimal("400")).compareTo(sonra.tahsilat().rezervasyon()));
        assertEquals(0, once.tahsilat().toplam().subtract(new BigDecimal("400")).compareTo(sonra.tahsilat().toplam()));
    }
}
