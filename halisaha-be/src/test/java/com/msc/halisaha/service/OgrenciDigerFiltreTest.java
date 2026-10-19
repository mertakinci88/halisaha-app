package com.msc.halisaha.service;

import com.msc.halisaha.TestOturum;
import com.msc.halisaha.common.AppZaman;
import com.msc.halisaha.dto.OgrenciFiltre;
import com.msc.halisaha.dto.OgrenciResponse;
import com.msc.halisaha.entity.Isletme;
import com.msc.halisaha.entity.Ogrenci;
import com.msc.halisaha.entity.OgrenciDurum;
import com.msc.halisaha.entity.OgrenciKayitFiltre;
import com.msc.halisaha.entity.OgrenciTransfer;
import com.msc.halisaha.entity.Rol;
import com.msc.halisaha.entity.TransferDurum;
import com.msc.halisaha.entity.Yoklama;
import com.msc.halisaha.entity.YoklamaDurum;
import com.msc.halisaha.repository.IsletmeRepository;
import com.msc.halisaha.repository.KullaniciRepository;
import com.msc.halisaha.repository.OgrenciRepository;
import com.msc.halisaha.repository.OgrenciTransferRepository;
import com.msc.halisaha.repository.YoklamaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Öğrenci listesinin kayıt tarihi, antrenman günü, transfer ve devamsızlık filtreleri. */
@SpringBootTest
@Transactional
class OgrenciDigerFiltreTest {

    @Autowired OgrenciService ogrenciService;
    @Autowired OgrenciRepository ogrenciRepository;
    @Autowired YoklamaRepository yoklamaRepository;
    @Autowired OgrenciTransferRepository transferRepository;
    @Autowired IsletmeRepository isletmeRepository;
    @Autowired KullaniciRepository kullaniciRepository;

    private Isletme isletme;

    @BeforeEach
    void hazirla() {
        isletme = TestOturum.isletme(isletmeRepository, "Diğer Filtreler");
        TestOturum.girisYap(TestOturum.kullanici(kullaniciRepository, isletme, Rol.ADMIN));
    }

    @AfterEach
    void temizle() {
        TestOturum.temizle();
    }

    private Ogrenci ogrenci(String ad, Integer antrenman, LocalDateTime kayit) {
        return ogrenciRepository.save(Ogrenci.builder().isletme(isletme).adSoyad(ad).durum(OgrenciDurum.AKTIF)
                .antrenmanGunSayisi(antrenman).kayitTarih(kayit).build());
    }

    private void yoklama(Ogrenci o, LocalDate tarih, YoklamaDurum durum) {
        yoklamaRepository.save(Yoklama.builder().isletme(isletme).ogrenci(o).tarih(tarih).durum(durum).build());
    }

    private List<String> adlar(OgrenciFiltre f) {
        return ogrenciService.list(f, 0, 50).icerik().stream().map(OgrenciResponse::adSoyad).toList();
    }

    private static OgrenciFiltre filtre(OgrenciKayitFiltre kayit, Integer antrenman, boolean transfer, Integer devamsizlik) {
        return new OgrenciFiltre(null, null, null, null, null, null, kayit, antrenman, transfer, devamsizlik);
    }

    @Test
    void kayitTarihiFiltresiBuAyVeSonUcAyiAyirir() {
        LocalDate bugun = AppZaman.bugun();
        ogrenci("Yeni", 2, bugun.atStartOfDay());
        ogrenci("İki ay önce", 2, bugun.withDayOfMonth(1).minusMonths(2).atStartOfDay());
        ogrenci("Eski", 2, bugun.withDayOfMonth(1).minusMonths(3).atStartOfDay());

        assertEquals(List.of("Yeni"), adlar(filtre(OgrenciKayitFiltre.BU_AY, null, false, null)));
        assertEquals(List.of("Yeni", "İki ay önce"), adlar(filtre(OgrenciKayitFiltre.SON_3_AY, null, false, null)));
        assertEquals(3, adlar(filtre(null, null, false, null)).size());
    }

    @Test
    void antrenmanGunuFiltresi() {
        ogrenci("İki gün", 2, null);
        ogrenci("Üç gün", 3, null);
        ogrenci("Belirsiz", null, null);

        assertEquals(List.of("Üç gün"), adlar(filtre(null, 3, false, null)));
        assertEquals(List.of("İki gün"), adlar(filtre(null, 2, false, null)));
        assertEquals(0, adlar(filtre(null, 4, false, null)).size());
    }

    @Test
    void transferBekleyenFiltresiYalnizcaBekleyenTalebiOlanlariGetirir() {
        Ogrenci bekleyen = ogrenci("Bekleyen", 2, null);
        Ogrenci onayli = ogrenci("Onaylı", 2, null);
        ogrenci("Talepsiz", 2, null);
        transferRepository.save(OgrenciTransfer.builder().isletme(isletme).ogrenci(bekleyen).hedefGrupAd("U12")
                .durum(TransferDurum.BEKLIYOR).build());
        transferRepository.save(OgrenciTransfer.builder().isletme(isletme).ogrenci(onayli).hedefGrupAd("U12")
                .durum(TransferDurum.ONAYLANDI).build());

        assertEquals(List.of("Bekleyen"), adlar(filtre(null, null, true, null)));
    }

    @Test
    void devamsizlikFiltresiSonOtuzGundekiGelmediSayisinaBakarIzinliSayilmaz() {
        LocalDate bugun = AppZaman.bugun();
        Ogrenci cok = ogrenci("Çok gelmeyen", 2, null);
        Ogrenci az = ogrenci("Az gelmeyen", 2, null);
        Ogrenci izinli = ogrenci("İzinli", 2, null);
        Ogrenci eski = ogrenci("Eski devamsız", 2, null);
        for (int i = 1; i <= 3; i++) yoklama(cok, bugun.minusDays(i), YoklamaDurum.GELMEDI);
        yoklama(az, bugun.minusDays(1), YoklamaDurum.GELMEDI);
        for (int i = 1; i <= 3; i++) yoklama(izinli, bugun.minusDays(i), YoklamaDurum.IZINLI);
        for (int i = 40; i <= 42; i++) yoklama(eski, bugun.minusDays(i), YoklamaDurum.GELMEDI);   // 30 günden eski

        assertEquals(List.of("Çok gelmeyen"), adlar(filtre(null, null, false, 3)));
        assertEquals(List.of("Az gelmeyen", "Çok gelmeyen"), adlar(filtre(null, null, false, 1)));
        assertEquals(4, adlar(filtre(null, null, false, null)).size());
    }
}
