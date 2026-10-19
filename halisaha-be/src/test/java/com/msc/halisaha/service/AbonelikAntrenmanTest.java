package com.msc.halisaha.service;

import com.msc.halisaha.TestOturum;
import com.msc.halisaha.common.AppZaman;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.dto.AbonelikGuncelleRequest;
import com.msc.halisaha.dto.AbonelikIptalRequest;
import com.msc.halisaha.dto.AbonelikOdemeRequest;
import com.msc.halisaha.dto.AbonelikOnizlemeRequest;
import com.msc.halisaha.dto.AbonelikRequest;
import com.msc.halisaha.dto.AbonelikResponse;
import com.msc.halisaha.dto.AntrenmanaCevirRequest;
import com.msc.halisaha.dto.OdemeRequest;
import com.msc.halisaha.dto.RezervasyonRaporu;
import com.msc.halisaha.entity.Isletme;
import com.msc.halisaha.entity.Kullanici;
import com.msc.halisaha.entity.OdemeDurumu;
import com.msc.halisaha.entity.OdemeTipi;
import com.msc.halisaha.entity.OdemeYontemi;
import com.msc.halisaha.entity.Rezervasyon;
import com.msc.halisaha.entity.RezervasyonDurum;
import com.msc.halisaha.entity.Rol;
import com.msc.halisaha.entity.Saha;
import com.msc.halisaha.entity.SahaDurum;
import com.msc.halisaha.repository.IsletmeRepository;
import com.msc.halisaha.repository.KullaniciRepository;
import com.msc.halisaha.repository.RezervasyonRepository;
import com.msc.halisaha.repository.SahaRepository;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class AbonelikAntrenmanTest {

    @Autowired AbonelikService abonelikService;
    @Autowired RezervasyonService rezervasyonService;
    @Autowired RezervasyonRaporService raporService;
    @Autowired IsletmeRepository isletmeRepository;
    @Autowired KullaniciRepository kullaniciRepository;
    @Autowired SahaRepository sahaRepository;
    @Autowired RezervasyonRepository rezervasyonRepository;

    private Saha saha;
    private LocalDate baslangic;

    @BeforeEach
    void hazirla() {
        Isletme isletme = TestOturum.isletme(isletmeRepository, "A İşletmesi");
        Kullanici k = TestOturum.kullanici(kullaniciRepository, isletme, Rol.ADMIN);
        saha = sahaRepository.save(Saha.builder().isletme(isletme).ad("Saha 1").durum(SahaDurum.AKTIF)
                .saatlikUcret(new BigDecimal("1000")).build());
        TestOturum.girisYap(k);
        baslangic = AppZaman.bugun().plusDays(3);
    }

    @AfterEach
    void temizle() {
        TestOturum.temizle();
    }

    private AbonelikRequest antrenmanIstek() {
        return new AbonelikRequest(saha.getId(), "Antrenman", "05469764613", baslangic.getDayOfWeek().getValue(), 1140, 60,
                baslangic, null, null, null, null, null, null, null, true);
    }

    private AbonelikRequest ucretliIstek() {
        return new AbonelikRequest(saha.getId(), "Ali Abone", "05469764613", baslangic.getDayOfWeek().getValue(), 1140, 60,
                baslangic, new BigDecimal("2000"), new BigDecimal("1000"), new BigDecimal("2000"), OdemeYontemi.NAKIT, null, null, null);
    }

    private Rezervasyon hafta(AbonelikResponse r, int sira) {
        return rezervasyonRepository.findById(r.haftalar().get(sira).rezervasyonId()).orElseThrow();
    }

    private ApiException hata(Runnable is) {
        return assertThrows(ApiException.class, is::run);
    }

    @Test
    void antrenmanAboneligiUcretsizHaftalikSaatKapatir() {
        AbonelikResponse r = abonelikService.olustur(antrenmanIstek());

        assertTrue(r.antrenman());
        assertEquals(0, BigDecimal.ZERO.compareTo(r.depozito()));
        assertEquals(0, BigDecimal.ZERO.compareTo(r.haftalikUcret()));
        assertEquals(OdemeDurumu.ODENDI, r.odemeDurumu());
        assertTrue(r.odemeler().isEmpty());
        assertEquals(52, r.haftalar().size());
        assertEquals(0, BigDecimal.ZERO.compareTo(r.haftalar().get(0).odenecekTutar()));
        assertEquals(OdemeDurumu.ODENDI, r.haftalar().get(0).odemeDurumu());
        assertTrue(hafta(r, 0).isAntrenman());

        // Saat dolu: aynı saate rezervasyon / abonelik açılamaz
        var onizleme = abonelikService.onizle(new AbonelikOnizlemeRequest(saha.getId(), baslangic.getDayOfWeek().getValue(), 1140, 60, baslangic));
        assertEquals(52, onizleme.cakismalar().size());

        // Ödeme alınamaz
        assertEquals(400, hata(() -> abonelikService.odemeEkle(r.id(),
                new AbonelikOdemeRequest(new BigDecimal("10"), OdemeYontemi.NAKIT, null, null))).getStatus().value());
        Long haftaId = r.haftalar().get(0).rezervasyonId();
        assertEquals(400, hata(() -> rezervasyonService.odemeEkle(haftaId,
                new OdemeRequest(OdemeTipi.ODEME, new BigDecimal("10"), OdemeYontemi.NAKIT, null))).getStatus().value());
    }

    @Test
    void ucretliAboneligindeAlanlarZorunluKalir() {
        AbonelikRequest eksik = new AbonelikRequest(saha.getId(), "Ali", "05469764613", baslangic.getDayOfWeek().getValue(), 1140, 60,
                baslangic, null, new BigDecimal("1000"), new BigDecimal("500"), OdemeYontemi.NAKIT, null, null, null);
        assertEquals(400, hata(() -> abonelikService.olustur(eksik)).getStatus().value());
    }

    @Test
    void antrenmanRezervasyonlariRaporlaraGirmez() {
        abonelikService.olustur(antrenmanIstek());
        rezervasyonRepository.flush();
        RezervasyonRaporu rapor = raporService.getir(baslangic, baslangic.plusWeeks(52));
        assertEquals(0, rapor.rezervasyon());
        assertEquals(0, BigDecimal.ZERO.compareTo(rapor.toplamSaat()));
    }

    @Test
    void antrenmanAboneliginiGuncellemekTutarIstemez() {
        AbonelikResponse r = abonelikService.olustur(antrenmanIstek());
        AbonelikResponse g = abonelikService.guncelle(r.id(), new AbonelikGuncelleRequest("Yeni Ad", "05469764613", null, null, "not"));
        assertEquals("Yeni Ad", g.adSoyad());
        assertEquals("Yeni Ad", hafta(g, 0).getAdSoyad());
        assertTrue(g.antrenman());
    }

    @Test
    void antrenmanIptaliTumGelecekHaftalariIptalEderIadeYoktur() {
        AbonelikResponse r = abonelikService.olustur(antrenmanIstek());
        AbonelikResponse iptal = abonelikService.iptalEt(r.id(), new AbonelikIptalRequest(null, null, null, null));
        assertEquals(0, iptal.haftalar().stream().filter(h -> h.durum() == RezervasyonDurum.AKTIF).count());
    }

    @Test
    void ucretliAbonelikAntrenmanaCevrilir() {
        AbonelikResponse r = abonelikService.olustur(ucretliIstek());

        // Depozito ödemesi var: onay (odemeleriSil) olmadan çevrilmez
        assertEquals(409, hata(() -> abonelikService.antrenmanaCevir(r.id(), new AntrenmanaCevirRequest(null))).getStatus().value());
        assertFalse(abonelikService.getir(r.id()).antrenman());

        AbonelikResponse c = abonelikService.antrenmanaCevir(r.id(), new AntrenmanaCevirRequest(true));
        assertTrue(c.antrenman());
        assertTrue(c.odemeler().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(c.depozito()));
        assertEquals(0, BigDecimal.ZERO.compareTo(c.haftalikUcret()));
        assertEquals(OdemeDurumu.ODENDI, c.odemeDurumu());
        for (AbonelikResponse.HaftaResponse h : c.haftalar()) {
            assertEquals(0, BigDecimal.ZERO.compareTo(h.odenecekTutar()));
            assertEquals(OdemeDurumu.ODENDI, h.odemeDurumu());
        }
        assertTrue(hafta(c, 51).isAntrenman());

        // Tekrar çevirmek zararsızdır
        assertTrue(abonelikService.antrenmanaCevir(r.id(), new AntrenmanaCevirRequest(true)).antrenman());
    }

    @Test
    void odemesiAlinmisHaftaVarsaCevrilmez() {
        AbonelikResponse r = abonelikService.olustur(ucretliIstek());
        rezervasyonService.odemeEkle(r.haftalar().get(2).rezervasyonId(),
                new OdemeRequest(OdemeTipi.ODEME, new BigDecimal("1000"), OdemeYontemi.NAKIT, null));
        assertEquals(409, hata(() -> abonelikService.antrenmanaCevir(r.id(), new AntrenmanaCevirRequest(true))).getStatus().value());
        assertFalse(abonelikService.getir(r.id()).antrenman());
    }

    @Test
    void gecmisHaftalarCevirmedeDegismez() {
        AbonelikResponse r = abonelikService.olustur(ucretliIstek());
        // İlk haftayı geçmişe taşı (bugünden önce): çevirme yalnızca bugünden sonrasını sıfırlar
        Rezervasyon ilk = hafta(r, 0);
        ilk.setBaslangicTarih(ilk.getBaslangicTarih().minusDays(30));
        ilk.setBitisTarih(ilk.getBitisTarih().minusDays(30));
        rezervasyonRepository.flush();

        AbonelikResponse c = abonelikService.antrenmanaCevir(r.id(), new AntrenmanaCevirRequest(true));
        List<AbonelikResponse.HaftaResponse> haftalar = c.haftalar();
        Rezervasyon gecmis = rezervasyonRepository.findById(haftalar.get(0).rezervasyonId()).orElseThrow();
        assertFalse(gecmis.isAntrenman());
        assertEquals(0, new BigDecimal("1000").compareTo(gecmis.getOdenecekTutar()));
        assertTrue(hafta(c, 1).isAntrenman());
    }
}
