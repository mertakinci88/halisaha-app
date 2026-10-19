package com.msc.halisaha.service;

import com.msc.halisaha.TestOturum;
import com.msc.halisaha.common.AppZaman;
import com.msc.halisaha.dto.OgrenciFiltre;
import com.msc.halisaha.dto.OgrenciResponse;
import com.msc.halisaha.entity.OgrenciOdemeFiltre;
import com.msc.halisaha.dto.PageResponse;
import com.msc.halisaha.entity.Isletme;
import com.msc.halisaha.entity.OgrenciOdemeDurumu;
import com.msc.halisaha.entity.OdemeYontemi;
import com.msc.halisaha.entity.Ogrenci;
import com.msc.halisaha.entity.OgrenciDurum;
import com.msc.halisaha.entity.OgrenciOdeme;
import com.msc.halisaha.entity.Rol;
import com.msc.halisaha.repository.IsletmeRepository;
import com.msc.halisaha.repository.KullaniciRepository;
import com.msc.halisaha.repository.OgrenciOdemeRepository;
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

@SpringBootTest
@Transactional
class OgrenciOdemeFiltreTest {

    @Autowired OgrenciService ogrenciService;
    @Autowired OgrenciRepository ogrenciRepository;
    @Autowired OgrenciOdemeRepository ogrenciOdemeRepository;
    @Autowired IsletmeRepository isletmeRepository;
    @Autowired KullaniciRepository kullaniciRepository;

    @BeforeEach
    void hazirla() {
        Isletme isletme = TestOturum.isletme(isletmeRepository, "Ödeme Filtre");
        TestOturum.girisYap(TestOturum.kullanici(kullaniciRepository, isletme, Rol.ADMIN));
        ogrenci(isletme, "Ödedi", "1000", true);
        ogrenci(isletme, "Ödemedi", null, true);
        ogrenci(isletme, "Kısmi", "300", false);
        ogrenciRepository.save(Ogrenci.builder().isletme(isletme).adSoyad("Pasif").durum(OgrenciDurum.PASIF).build());
    }

    @AfterEach
    void temizle() {
        TestOturum.temizle();
    }

    /** odenen: bu ay girilen tutar (null = ödeme girilmemiş); tamam: ödeme ayı tamamlıyor mu. */
    private void ogrenci(Isletme isletme, String ad, String odenen, boolean tamam) {
        Ogrenci o = ogrenciRepository.save(Ogrenci.builder().isletme(isletme).adSoyad(ad).durum(OgrenciDurum.AKTIF).build());
        if (odenen != null) {
            LocalDate bugun = AppZaman.bugun();
            ogrenciOdemeRepository.save(OgrenciOdeme.builder().ogrenci(o).yil(bugun.getYear()).ay(bugun.getMonthValue())
                    .odenenTutar(new BigDecimal(odenen)).tamam(tamam)
                    .odemeSekli(OdemeYontemi.NAKIT).odemeTarihi(bugun).build());
        }
    }

    private List<OgrenciResponse> liste(boolean odemeBekleyen) {
        PageResponse<OgrenciResponse> sayfa = ogrenciService.list(null, null, odemeBekleyen, false, 0, 50);
        return sayfa.icerik();
    }

    @Test
    void filtreKapaliysaHepsiAcikSaBuAyiTamamlanmamisOlanlarGelir() {
        assertEquals(List.of("Kısmi", "Pasif", "Ödedi", "Ödemedi"), liste(false).stream().map(OgrenciResponse::adSoyad).toList());
        assertEquals(List.of("Kısmi", "Pasif", "Ödemedi"), liste(true).stream().map(OgrenciResponse::adSoyad).toList());
    }

    @Test
    void odemeDurumuBuAyinOdemesindenTuretilir() {
        List<OgrenciResponse> hepsi = liste(false);
        boolean gecti = AppZaman.bugun().getDayOfMonth() > OgrenciOdemeDurumu.ODEME_GUNU;
        assertEquals(gecti ? OgrenciOdemeDurumu.KISMI_GECIKMIS : OgrenciOdemeDurumu.KISMI_ODENDI, hepsi.get(0).odemeDurumu());
        assertEquals(OgrenciOdemeDurumu.ODENDI, hepsi.get(2).odemeDurumu());
        assertEquals(gecti ? OgrenciOdemeDurumu.ODENMEDI : OgrenciOdemeDurumu.ODEME_BEKLIYOR, hepsi.get(3).odemeDurumu());
    }

    private List<String> adlar(OgrenciFiltre filtre) {
        return ogrenciService.list(filtre, 0, 50).icerik().stream().map(OgrenciResponse::adSoyad).toList();
    }

    @Test
    void durumFiltresiAktifPasifVeTumunuAyirir() {
        assertEquals(List.of("Pasif"), adlar(new OgrenciFiltre(null, null, OgrenciDurum.PASIF, null, false)));
        assertEquals(List.of("Kısmi", "Ödedi", "Ödemedi"), adlar(new OgrenciFiltre(null, null, OgrenciDurum.AKTIF, null, false)));
        assertEquals(4, adlar(new OgrenciFiltre(null, null, null, null, false)).size());
    }

    @Test
    void odemeFiltreleriHicKismiVeGecikmisiAyirir() {
        OgrenciDurum aktif = OgrenciDurum.AKTIF;
        assertEquals(List.of("Ödedi"), adlar(new OgrenciFiltre(null, null, aktif, OgrenciOdemeFiltre.ODENDI, false)));
        assertEquals(List.of("Kısmi", "Ödemedi"), adlar(new OgrenciFiltre(null, null, aktif, OgrenciOdemeFiltre.BEKLEYEN, false)));
        assertEquals(List.of("Ödemedi"), adlar(new OgrenciFiltre(null, null, aktif, OgrenciOdemeFiltre.HIC, false)));
        assertEquals(List.of("Kısmi"), adlar(new OgrenciFiltre(null, null, aktif, OgrenciOdemeFiltre.KISMI, false)));
        List<String> gecikmis = AppZaman.bugun().getDayOfMonth() > OgrenciOdemeDurumu.ODEME_GUNU ? List.of("Kısmi", "Ödemedi") : List.of();
        assertEquals(gecikmis, adlar(new OgrenciFiltre(null, null, aktif, OgrenciOdemeFiltre.GECIKMIS, false)));
    }
}
