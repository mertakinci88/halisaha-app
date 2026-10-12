package com.msc.halisaha.service;

import com.msc.halisaha.TestOturum;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.dto.HesapRequest;
import com.msc.halisaha.dto.OgrenciOdemeAyRequest;
import com.msc.halisaha.dto.OgrenciOdemeKaydetRequest;
import com.msc.halisaha.dto.OgrenciOdemeYilResponse;
import com.msc.halisaha.entity.HesapDurum;
import com.msc.halisaha.entity.Isletme;
import com.msc.halisaha.entity.OgrenciOdemeDurumu;
import com.msc.halisaha.entity.OdemeYontemi;
import com.msc.halisaha.entity.Ogrenci;
import com.msc.halisaha.entity.OgrenciDurum;
import com.msc.halisaha.entity.Rol;
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
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class OgrenciOdemeServiceTest {

    private static final String IBAN = "TR33 0006 1005 1978 6457 8413 26";

    @Autowired OgrenciOdemeService odemeService;
    @Autowired HesapService hesapService;
    @Autowired OgrenciRepository ogrenciRepository;
    @Autowired IsletmeRepository isletmeRepository;
    @Autowired KullaniciRepository kullaniciRepository;

    private Isletme isletme;
    private Ogrenci ogrenci;

    @BeforeEach
    void hazirla() {
        isletme = TestOturum.isletme(isletmeRepository, "Aylık Ödeme");
        TestOturum.girisYap(TestOturum.kullanici(kullaniciRepository, isletme, Rol.ADMIN));
        ogrenci = ogrenciRepository.save(Ogrenci.builder().isletme(isletme).adSoyad("Ali").durum(OgrenciDurum.AKTIF).build());
    }

    @AfterEach
    void temizle() {
        TestOturum.temizle();
    }

    private static OgrenciOdemeAyRequest ay(int ay, String tutar, OdemeYontemi sekil, Long hesapId) {
        return new OgrenciOdemeAyRequest(ay, tutar != null ? new BigDecimal(tutar) : null, sekil, hesapId, LocalDate.of(2030, ay, 5), null);
    }

    private OgrenciOdemeYilResponse kaydet(OgrenciOdemeAyRequest... aylar) {
        return odemeService.kaydet(ogrenci.getId(), 2030, new OgrenciOdemeKaydetRequest(List.of(aylar)));
    }

    @Test
    void yilHerZamanOnIkiAyDondurur() {
        OgrenciOdemeYilResponse r = odemeService.getir(ogrenci.getId(), 2030);
        assertEquals(12, r.aylar().size());
        assertEquals(OgrenciOdemeDurumu.ODEME_BEKLIYOR, r.aylar().get(0).durum());
        assertEquals(0, BigDecimal.ZERO.compareTo(r.toplamOdenen()));
    }

    @Test
    void ayAyKaydedilirTutarVarsaOdendiYoksaOdenmedi() {
        OgrenciOdemeYilResponse r = kaydet(ay(1, "1000", OdemeYontemi.NAKIT, null), ay(2, "400", OdemeYontemi.KART, null));
        assertEquals(OgrenciOdemeDurumu.ODENDI, r.aylar().get(0).durum());
        assertEquals(OgrenciOdemeDurumu.ODENDI, r.aylar().get(1).durum());          // tam / kısmi ayrımı yok
        assertEquals(OdemeYontemi.KART, r.aylar().get(1).odemeSekli());
        assertEquals(OgrenciOdemeDurumu.ODEME_BEKLIYOR, r.aylar().get(2).durum());
        assertEquals(0, new BigDecimal("1400").compareTo(r.toplamOdenen()));
    }

    @Test
    void tutarinUstSiniriYoktur() {
        OgrenciOdemeYilResponse r = kaydet(ay(3, "25000.50", OdemeYontemi.NAKIT, null));
        assertEquals(0, new BigDecimal("25000.50").compareTo(r.aylar().get(2).odenenTutar()));
    }

    @Test
    void notKaydedilirBosGelinceTemizlenir() {
        var notlu = new OgrenciOdemeAyRequest(8, new BigDecimal("500"), OdemeYontemi.NAKIT, null, LocalDate.of(2030, 8, 5), "  Babası getirdi ");
        assertEquals("Babası getirdi", kaydet(notlu).aylar().get(7).not());
        var notsuz = new OgrenciOdemeAyRequest(8, new BigDecimal("500"), OdemeYontemi.NAKIT, null, LocalDate.of(2030, 8, 5), " ");
        assertNull(kaydet(notsuz).aylar().get(7).not());
    }

    @Test
    void havaleIcinHesapZorunluDigerYontemlerdeTutulmaz() {
        assertEquals(400, assertThrows(ApiException.class, () -> kaydet(ay(6, "100", OdemeYontemi.HAVALE_EFT, null))).getStatus().value());
        var hesap = hesapService.create(new HesapRequest("Garanti", IBAN, "Ali Veli", HesapDurum.AKTIF));
        OgrenciOdemeYilResponse havale = kaydet(ay(6, "100", OdemeYontemi.HAVALE_EFT, hesap.id()));
        assertEquals(hesap.id(), havale.aylar().get(5).hesapId());
        OgrenciOdemeYilResponse nakit = kaydet(ay(6, "100", OdemeYontemi.NAKIT, hesap.id()));
        assertNull(nakit.aylar().get(5).hesapId());
    }

    @Test
    void ayniAyIkiKezGonderilemez() {
        assertEquals(400, assertThrows(ApiException.class,
                () -> kaydet(ay(7, "100", OdemeYontemi.NAKIT, null), ay(7, "200", OdemeYontemi.NAKIT, null))).getStatus().value());
    }

    @Test
    void baskaIsletmeninOgrencisineErisilemez() {
        Isletme diger = TestOturum.isletme(isletmeRepository, "Diğer");
        TestOturum.girisYap(TestOturum.kullanici(kullaniciRepository, diger, Rol.ADMIN));
        assertEquals(404, assertThrows(ApiException.class, () -> odemeService.getir(ogrenci.getId(), 2030)).getStatus().value());
    }

    @Test
    void sadeceNotKaydedilebilirAyOdenmisSayilmaz() {
        var sadeceNot = new OgrenciOdemeAyRequest(9, null, null, null, null, "İzinli, ödeme sonra");
        OgrenciOdemeYilResponse r = kaydet(sadeceNot);
        assertEquals("İzinli, ödeme sonra", r.aylar().get(8).not());
        assertEquals(OgrenciOdemeDurumu.ODEME_BEKLIYOR, r.aylar().get(8).durum());      // 2030: vadesi gelmemiş
        assertNull(r.aylar().get(8).odemeSekli());
        assertEquals(0, BigDecimal.ZERO.compareTo(r.aylar().get(8).odenenTutar()));
        // not da boşaltılırsa kayıt silinir
        OgrenciOdemeYilResponse silinmis = kaydet(new OgrenciOdemeAyRequest(9, null, null, null, null, " "));
        assertNull(silinmis.aylar().get(8).not());
    }

    @Test
    void tutarVarsaOdemeSekliSecilmelidir() {
        assertEquals(400, assertThrows(ApiException.class, () -> kaydet(ay(10, "100", null, null))).getStatus().value());
    }

    @Test
    void notlaBirlikteTutarGirilinceOdendiOlur() {
        var tam = new OgrenciOdemeAyRequest(11, new BigDecimal("300"), OdemeYontemi.KART, null, LocalDate.of(2030, 11, 2), "Not");
        OgrenciOdemeYilResponse r = kaydet(tam);
        assertEquals(OgrenciOdemeDurumu.ODENDI, r.aylar().get(10).durum());
        assertEquals("Not", r.aylar().get(10).not());
    }
}
