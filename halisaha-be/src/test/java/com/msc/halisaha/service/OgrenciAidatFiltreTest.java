package com.msc.halisaha.service;

import com.msc.halisaha.TestOturum;
import com.msc.halisaha.dto.OgrenciResponse;
import com.msc.halisaha.dto.PageResponse;
import com.msc.halisaha.entity.Isletme;
import com.msc.halisaha.entity.OdemeDurumu;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class OgrenciAidatFiltreTest {

    @Autowired OgrenciService ogrenciService;
    @Autowired OgrenciRepository ogrenciRepository;
    @Autowired IsletmeRepository isletmeRepository;
    @Autowired KullaniciRepository kullaniciRepository;

    @BeforeEach
    void hazirla() {
        Isletme isletme = TestOturum.isletme(isletmeRepository, "Aidat Filtre");
        TestOturum.girisYap(TestOturum.kullanici(kullaniciRepository, isletme, Rol.ADMIN));
        ogrenci(isletme, "Ödedi", OdemeDurumu.ODENDI);
        ogrenci(isletme, "Ödemedi", OdemeDurumu.ODENMEDI);
        ogrenci(isletme, "Kısmi", OdemeDurumu.KISMI_ODENDI);
    }

    @AfterEach
    void temizle() {
        TestOturum.temizle();
    }

    private void ogrenci(Isletme isletme, String ad, OdemeDurumu durum) {
        ogrenciRepository.save(Ogrenci.builder().isletme(isletme).adSoyad(ad).durum(OgrenciDurum.AKTIF).odemeDurumu(durum).build());
    }

    private List<String> adlar(boolean aidatBekleyen) {
        PageResponse<OgrenciResponse> sayfa = ogrenciService.list(null, null, aidatBekleyen, 0, 50);
        return sayfa.icerik().stream().map(OgrenciResponse::adSoyad).toList();
    }

    @Test
    void filtreKapaliysaHepsiAcikSaOdemesiTamamlanmayanlarGelir() {
        assertEquals(List.of("Kısmi", "Ödedi", "Ödemedi"), adlar(false));
        assertEquals(List.of("Kısmi", "Ödemedi"), adlar(true));
    }
}
