package com.msc.halisaha.service;

import com.msc.halisaha.TestOturum;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.dto.HesapRequest;
import com.msc.halisaha.dto.OgrenciOdemeAyResponse;
import com.msc.halisaha.dto.OgrenciOdemeRequest;
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

    private static OgrenciOdemeRequest odeme(String tutar, OdemeYontemi sekil, Long hesapId, boolean tamam) {
        return new OgrenciOdemeRequest(tutar != null ? new BigDecimal(tutar) : null, sekil, hesapId, LocalDate.of(2030, 1, 5), null, tamam);
    }

    private OgrenciOdemeYilResponse ekle(int ay, OgrenciOdemeRequest istek) {
        return odemeService.ekle(ogrenci.getId(), 2030, ay, istek);
    }

    private static OgrenciOdemeAyResponse ay(OgrenciOdemeYilResponse r, int ay) {
        return r.aylar().get(ay - 1);
    }

    @Test
    void yilHerZamanOnIkiAyDondurur() {
        OgrenciOdemeYilResponse r = odemeService.getir(ogrenci.getId(), 2030);
        assertEquals(12, r.aylar().size());
        assertEquals(OgrenciOdemeDurumu.ODEME_BEKLIYOR, ay(r, 1).durum());
        assertEquals(0, BigDecimal.ZERO.compareTo(r.toplamOdenen()));
    }

    @Test
    void tamamOdemeAyiOdendiYapar() {
        OgrenciOdemeYilResponse r = ekle(1, odeme("1000", OdemeYontemi.NAKIT, null, true));
        assertEquals(OgrenciOdemeDurumu.ODENDI, ay(r, 1).durum());
        assertEquals(OgrenciOdemeDurumu.ODEME_BEKLIYOR, ay(r, 2).durum());
        assertEquals(0, new BigDecimal("1000").compareTo(r.toplamOdenen()));
    }

    @Test
    void kismiOdemelerAyiAcikTutarTamamOdemeKapatir() {
        OgrenciOdemeYilResponse r = ekle(2, odeme("1000", OdemeYontemi.NAKIT, null, false));
        assertEquals(OgrenciOdemeDurumu.KISMI_ODENDI, ay(r, 2).durum());
        assertEquals(0, new BigDecimal("1000").compareTo(ay(r, 2).odenenTutar()));

        r = ekle(2, odeme("500", OdemeYontemi.KART, null, false));
        assertEquals(OgrenciOdemeDurumu.KISMI_ODENDI, ay(r, 2).durum());
        assertEquals(2, ay(r, 2).odemeler().size());
        assertEquals(0, new BigDecimal("1500").compareTo(ay(r, 2).odenenTutar()));

        r = ekle(2, odeme("1500", OdemeYontemi.NAKIT, null, true));
        assertEquals(OgrenciOdemeDurumu.ODENDI, ay(r, 2).durum());
        assertEquals(3, ay(r, 2).odemeler().size());
        assertEquals(0, new BigDecimal("3000").compareTo(r.toplamOdenen()));
    }

    @Test
    void tamamOdemeSilinincedenAyTekrarKismiOlur() {
        ekle(3, odeme("400", OdemeYontemi.NAKIT, null, false));
        OgrenciOdemeYilResponse r = ekle(3, odeme("600", OdemeYontemi.NAKIT, null, true));
        assertEquals(OgrenciOdemeDurumu.ODENDI, ay(r, 3).durum());
        Long tamamId = ay(r, 3).odemeler().get(1).id();
        r = odemeService.sil(ogrenci.getId(), 2030, 3, tamamId);
        assertEquals(OgrenciOdemeDurumu.KISMI_ODENDI, ay(r, 3).durum());
        assertEquals(1, ay(r, 3).odemeler().size());
    }

    @Test
    void odemeGuncellenirTamamKismiDegisebilir() {
        OgrenciOdemeYilResponse r = ekle(4, odeme("700", OdemeYontemi.NAKIT, null, false));
        Long id = ay(r, 4).odemeler().get(0).id();
        r = odemeService.guncelle(ogrenci.getId(), 2030, 4, id, odeme("900", OdemeYontemi.KART, null, true));
        assertEquals(OgrenciOdemeDurumu.ODENDI, ay(r, 4).durum());
        assertEquals(0, new BigDecimal("900").compareTo(ay(r, 4).odenenTutar()));
        assertEquals(OdemeYontemi.KART, ay(r, 4).odemeler().get(0).odemeSekli());
        // yanlış ay ile erişim bulunamaz
        assertEquals(404, assertThrows(ApiException.class, () -> odemeService.sil(ogrenci.getId(), 2030, 5, id)).getStatus().value());
    }

    @Test
    void parasizKapatmaAciklamaIleAyiOdendiYapar() {
        var parasiz = new OgrenciOdemeRequest(null, null, null, null, "Burslu", true);
        OgrenciOdemeYilResponse r = ekle(5, parasiz);
        assertEquals(OgrenciOdemeDurumu.ODENDI, ay(r, 5).durum());
        assertEquals(0, BigDecimal.ZERO.compareTo(ay(r, 5).odenenTutar()));
        assertNull(ay(r, 5).odemeler().get(0).odemeSekli());
        assertNull(ay(r, 5).odemeler().get(0).odemeTarihi());
        // açıklamasız parasız kayıt reddedilir
        assertEquals(400, assertThrows(ApiException.class, () -> ekle(6, new OgrenciOdemeRequest(null, null, null, null, " ", true))).getStatus().value());
    }

    @Test
    void sadeceNotAyiOdenmisSaymaz() {
        OgrenciOdemeYilResponse r = ekle(9, new OgrenciOdemeRequest(null, null, null, null, "İzinli, ödeme sonra", false));
        assertEquals("İzinli, ödeme sonra", ay(r, 9).odemeler().get(0).not());
        assertEquals(OgrenciOdemeDurumu.ODEME_BEKLIYOR, ay(r, 9).durum());      // 2030: vadesi gelmemiş, tutar yok
    }

    @Test
    void tutarinUstSiniriYoktur() {
        OgrenciOdemeYilResponse r = ekle(7, odeme("25000.50", OdemeYontemi.NAKIT, null, true));
        assertEquals(0, new BigDecimal("25000.50").compareTo(ay(r, 7).odenenTutar()));
    }

    @Test
    void notTemizlenir() {
        var notlu = new OgrenciOdemeRequest(new BigDecimal("500"), OdemeYontemi.NAKIT, null, LocalDate.of(2030, 8, 5), "  Babası getirdi ", true);
        assertEquals("Babası getirdi", ay(ekle(8, notlu), 8).odemeler().get(0).not());
    }

    @Test
    void havaleIcinHesapZorunluDigerYontemlerdeTutulmaz() {
        assertEquals(400, assertThrows(ApiException.class, () -> ekle(6, odeme("100", OdemeYontemi.HAVALE_EFT, null, true))).getStatus().value());
        var hesap = hesapService.create(new HesapRequest("Garanti", IBAN, "Ali Veli", HesapDurum.AKTIF));
        OgrenciOdemeYilResponse havale = ekle(6, odeme("100", OdemeYontemi.HAVALE_EFT, hesap.id(), true));
        assertEquals(hesap.id(), ay(havale, 6).odemeler().get(0).hesapId());
        OgrenciOdemeYilResponse nakit = ekle(10, odeme("100", OdemeYontemi.NAKIT, hesap.id(), true));
        assertNull(ay(nakit, 10).odemeler().get(0).hesapId());
    }

    @Test
    void tutarVarsaOdemeSekliSecilmelidir() {
        assertEquals(400, assertThrows(ApiException.class, () -> ekle(10, odeme("100", null, null, true))).getStatus().value());
    }

    @Test
    void baskaIsletmeninOgrencisineErisilemez() {
        Isletme diger = TestOturum.isletme(isletmeRepository, "Diğer");
        TestOturum.girisYap(TestOturum.kullanici(kullaniciRepository, diger, Rol.ADMIN));
        assertEquals(404, assertThrows(ApiException.class, () -> odemeService.getir(ogrenci.getId(), 2030)).getStatus().value());
    }
}
