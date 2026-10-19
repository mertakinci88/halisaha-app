package com.msc.halisaha.service;

import com.msc.halisaha.TestOturum;
import com.msc.halisaha.common.AppZaman;
import com.msc.halisaha.dto.OgrenciAylikRaporu;
import com.msc.halisaha.dto.OgrenciRaporu;
import com.msc.halisaha.entity.Isletme;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Öğrenci raporunda ödeyen (ayı tamamlayan), kısmi ödeyen ve hiç ödemeyen ayrımı. */
@SpringBootTest
@Transactional
class OgrenciRaporServiceTest {

    @Autowired OgrenciRaporService raporService;
    @Autowired OgrenciRepository ogrenciRepository;
    @Autowired OgrenciOdemeRepository odemeRepository;
    @Autowired IsletmeRepository isletmeRepository;
    @Autowired KullaniciRepository kullaniciRepository;

    private final LocalDate bugun = AppZaman.bugun();

    @BeforeEach
    void hazirla() {
        Isletme isletme = TestOturum.isletme(isletmeRepository, "Öğrenci Rapor");
        TestOturum.girisYap(TestOturum.kullanici(kullaniciRepository, isletme, Rol.ADMIN));
        ogrenci(isletme, "Tam", "1000", true);
        ogrenci(isletme, "Parasız", "0", true);
        ogrenci(isletme, "Kısmi", "400", false);
        ogrenci(isletme, "Hiç", null, false);
    }

    @AfterEach
    void temizle() {
        TestOturum.temizle();
    }

    private void ogrenci(Isletme isletme, String ad, String tutar, boolean tamam) {
        Ogrenci o = ogrenciRepository.save(Ogrenci.builder().isletme(isletme).adSoyad(ad).durum(OgrenciDurum.AKTIF).build());
        if (tutar == null) {
            return;
        }
        boolean parali = new BigDecimal(tutar).signum() > 0;
        odemeRepository.save(OgrenciOdeme.builder().ogrenci(o).yil(bugun.getYear()).ay(bugun.getMonthValue())
                .odenenTutar(new BigDecimal(tutar)).tamam(tamam)
                .odemeSekli(parali ? OdemeYontemi.NAKIT : null).odemeTarihi(parali ? bugun : null)
                .not(parali ? null : "Burslu").build());
    }

    @Test
    void ayOzetiOdeyenKismiVeHicOdemeyeniAyirir() {
        OgrenciRaporu r = raporService.getir(bugun.getYear(), bugun.getMonthValue());
        assertEquals(4, r.aktifOgrenci());
        assertEquals(2, r.odeyen());              // Tam + parasız kapatılan
        assertEquals(2, r.odemeyen());            // Kısmi + Hiç
        assertEquals(1, r.kismiOdeyen());         // yalnızca Kısmi
        assertEquals(0, new BigDecimal("50.0").compareTo(r.tahsilatOrani()));
    }

    @Test
    void aylikRaporAyBazindaKismiSayisiniVerir() {
        OgrenciAylikRaporu r = raporService.aylik(bugun.withDayOfMonth(1), bugun);
        OgrenciAylikRaporu.AySatiri ay = r.aylar().get(0);
        assertEquals(4, ay.ogrenci());
        assertEquals(2, ay.odeyen());
        assertEquals(2, ay.odemeyen());
        assertEquals(1, ay.kismi());
        // toplam tutar kısmi ödemeleri de içerir: 1000 + 0 + 400
        assertEquals(0, new BigDecimal("1400").compareTo(r.toplamTutar()));
    }
}
