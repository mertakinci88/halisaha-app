package com.msc.halisaha.service;

import com.msc.halisaha.TestOturum;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.dto.HesapRequest;
import com.msc.halisaha.dto.HesapResponse;
import com.msc.halisaha.dto.OdemeRequest;
import com.msc.halisaha.dto.RezervasyonRequest;
import com.msc.halisaha.dto.RezervasyonResponse;
import com.msc.halisaha.entity.HesapDurum;
import com.msc.halisaha.entity.Isletme;
import com.msc.halisaha.entity.Kullanici;
import com.msc.halisaha.entity.OdemeTipi;
import com.msc.halisaha.entity.OdemeYontemi;
import com.msc.halisaha.entity.Rol;
import com.msc.halisaha.entity.Saha;
import com.msc.halisaha.entity.SahaDurum;
import com.msc.halisaha.repository.HesapRepository;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class HesapServiceTest {

    private static final String IBAN = "TR33 0006 1005 1978 6457 8413 26";   // geçerli (mod-97)
    private static final String IBAN_NORMAL = "TR330006100519786457841326";

    @Autowired HesapService hesapService;
    @Autowired RezervasyonService rezervasyonService;
    @Autowired HesapRepository hesapRepository;
    @Autowired SahaRepository sahaRepository;
    @Autowired IsletmeRepository isletmeRepository;
    @Autowired KullaniciRepository kullaniciRepository;

    private Isletme isletmeA;
    private Kullanici adminA;
    private Kullanici adminB;
    private Saha sahaA;

    @BeforeEach
    void hazirla() {
        isletmeA = TestOturum.isletme(isletmeRepository, "Hesap A");
        Isletme isletmeB = TestOturum.isletme(isletmeRepository, "Hesap B");
        adminA = TestOturum.kullanici(kullaniciRepository, isletmeA, Rol.ADMIN);
        adminB = TestOturum.kullanici(kullaniciRepository, isletmeB, Rol.ADMIN);
        sahaA = sahaRepository.save(Saha.builder().isletme(isletmeA).ad("Saha").durum(SahaDurum.AKTIF)
                .saatlikUcret(new BigDecimal("1000.00")).build());
        TestOturum.girisYap(adminA);
    }

    @AfterEach
    void temizle() {
        TestOturum.temizle();
    }

    private HesapResponse hesapAc() {
        return hesapService.create(new HesapRequest("Garanti X Şubesi", IBAN, "Ali Veli", null));
    }

    private RezervasyonResponse rezervasyon(int gun, OdemeYontemi yontem, Long hesapId) {
        LocalDateTime bas = LocalDateTime.of(2030, 2, gun, 14, 0);
        return rezervasyonService.create(new RezervasyonRequest(sahaA.getId(), "Müşteri", bas, bas.plusHours(1),
                "05551112233", yontem, new BigDecimal("200"), hesapId, null));
    }

    private int durum(Runnable is) {
        return assertThrows(ApiException.class, is::run).getStatus().value();
    }

    @Test
    void ibanBoslukTemizlenirVeAktifOlusur() {
        HesapResponse h = hesapAc();
        assertEquals(IBAN_NORMAL, h.iban());
        assertEquals(HesapDurum.AKTIF, h.durum());
    }

    @Test
    void hesapNumarasindaHarfOlanIbanKabulEdilirKucukHarfBuyutulur() {
        // TR IBAN'ın son 16 hanesi (hesap no) harf içerebilir
        HesapResponse h = hesapService.create(new HesapRequest("Harfli", "tr74 0006 1005 A978 6457 8413 2B", "Ali Veli", null));
        assertEquals("TR7400061005A978645784132B", h.iban());
    }

    @Test
    void gecersizIbanReddedilir() {
        assertEquals(400, durum(() -> hesapService.create(new HesapRequest("X", "TR12", "Ali", null))));
        // son hane bozuk: mod-97 sağlaması tutmaz
        assertEquals(400, durum(() -> hesapService.create(new HesapRequest("X", "TR330006100519786457841327", "Ali", null))));
    }

    @Test
    void baskaIsletmeHesabiniGoremezVeKullanamaz() {
        HesapResponse h = hesapAc();
        TestOturum.girisYap(adminB);
        assertTrue(hesapService.list().isEmpty());
        assertEquals(404, durum(() -> hesapService.getById(h.id())));
        TestOturum.girisYap(adminA);
        // B işletmesinin hesabı A'nın ödemesine bağlanamaz
        TestOturum.girisYap(adminB);
        HesapResponse hb = hesapAc();
        TestOturum.girisYap(adminA);
        assertEquals(404, durum(() -> rezervasyon(3, OdemeYontemi.HAVALE_EFT, hb.id())));
    }

    @Test
    void havaleKaporasiHesabiKaydederDigerYontemlerdeHesapYokSayilir() {
        HesapResponse h = hesapAc();
        RezervasyonResponse havale = rezervasyon(4, OdemeYontemi.HAVALE_EFT, h.id());
        assertEquals(h.id(), havale.odemeler().get(0).hesapId());
        assertEquals("Garanti X Şubesi", havale.odemeler().get(0).hesapAd());

        RezervasyonResponse nakit = rezervasyon(5, OdemeYontemi.NAKIT, h.id());
        assertNull(nakit.odemeler().get(0).hesapId());
    }

    @Test
    void tamOdemeRezervasyonTutarininTamaminiOdemeTipiyleTahsilEderHesabiKaydeder() {
        HesapResponse h = hesapAc();
        LocalDateTime bas = LocalDateTime.of(2030, 2, 10, 14, 0);
        RezervasyonResponse r = rezervasyonService.create(new RezervasyonRequest(sahaA.getId(), "Müşteri", bas, bas.plusHours(1),
                "05551112233", OdemeYontemi.HAVALE_EFT, new BigDecimal("200"), h.id(), true));
        assertEquals(1, r.odemeler().size());
        assertEquals(OdemeTipi.ODEME, r.odemeler().get(0).tip());
        assertEquals(0, new BigDecimal("1000.00").compareTo(r.odemeler().get(0).tutar()));   // kapora (200) yok sayılır
        assertEquals(h.id(), r.odemeler().get(0).hesapId());
        assertEquals(com.msc.halisaha.entity.OdemeDurumu.ODENDI, r.odemeDurumu());
    }

    @Test
    void havaleOdemesindeHesapSecimiZorunludur() {
        assertEquals(400, durum(() -> rezervasyon(9, OdemeYontemi.HAVALE_EFT, null)));
    }

    @Test
    void kayitSonrasiKaporaAlinamazKismiOdemeKaporaOlmadanAlinir() {
        LocalDateTime bas = LocalDateTime.of(2030, 2, 11, 14, 0);
        RezervasyonResponse r = rezervasyonService.create(new RezervasyonRequest(sahaA.getId(), "Müşteri", bas, bas.plusHours(1),
                "05551112233", null, null, null, null));   // peşin ödeme yok
        assertEquals(400, durum(() -> rezervasyonService.odemeEkle(r.id(),
                new OdemeRequest(OdemeTipi.KAPORA, new BigDecimal("100"), OdemeYontemi.NAKIT, null))));
        RezervasyonResponse guncel = rezervasyonService.odemeEkle(r.id(),
                new OdemeRequest(OdemeTipi.KISMI, new BigDecimal("100"), OdemeYontemi.NAKIT, null));
        assertEquals(OdemeTipi.KISMI, guncel.odemeler().get(0).tip());
    }

    @Test
    void sonradanAlinanOdemedeDeHesapSecilebilir() {
        HesapResponse h = hesapAc();
        RezervasyonResponse r = rezervasyon(6, OdemeYontemi.NAKIT, null);
        RezervasyonResponse guncel = rezervasyonService.odemeEkle(r.id(),
                new OdemeRequest(OdemeTipi.KISMI, new BigDecimal("100"), OdemeYontemi.HAVALE_EFT, h.id()));
        assertEquals(h.id(), guncel.odemeler().get(1).hesapId());
    }

    @Test
    void pasifHesapYeniOdemedeSecilemez() {
        HesapResponse h = hesapAc();
        hesapService.update(h.id(), new HesapRequest("Garanti X Şubesi", IBAN, "Ali Veli", HesapDurum.PASIF));
        assertEquals(400, durum(() -> rezervasyon(7, OdemeYontemi.HAVALE_EFT, h.id())));
    }

    @Test
    void kullanilmamisHesapSilinirKullanilmisOlanPasifeAlinir() {
        HesapResponse bos = hesapAc();
        hesapService.delete(bos.id());
        assertFalse(hesapRepository.existsById(bos.id()));

        HesapResponse kullanilan = hesapAc();
        rezervasyon(8, OdemeYontemi.HAVALE_EFT, kullanilan.id());
        hesapService.delete(kullanilan.id());
        assertEquals(HesapDurum.PASIF, hesapService.getById(kullanilan.id()).durum());
    }
}
