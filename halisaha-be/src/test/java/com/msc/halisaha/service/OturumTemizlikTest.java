package com.msc.halisaha.service;

import com.msc.halisaha.common.AppZaman;
import com.msc.halisaha.entity.Isletme;
import com.msc.halisaha.entity.Kullanici;
import com.msc.halisaha.entity.KullaniciDurum;
import com.msc.halisaha.entity.OturumLog;
import com.msc.halisaha.entity.OturumOlay;
import com.msc.halisaha.entity.RefreshToken;
import com.msc.halisaha.entity.Rol;
import com.msc.halisaha.repository.IsletmeRepository;
import com.msc.halisaha.repository.KullaniciRepository;
import com.msc.halisaha.repository.OturumLogRepository;
import com.msc.halisaha.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class OturumTemizlikTest {

    @Autowired OturumTemizlikService temizlik;
    @Autowired IsletmeRepository isletmeRepository;
    @Autowired KullaniciRepository kullaniciRepository;
    @Autowired OturumLogRepository oturumLogRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;

    private Kullanici kullanici;

    @BeforeEach
    void hazirla() {
        Isletme isletme = isletmeRepository.save(Isletme.builder().ad("Temizlik Test").build());
        kullanici = kullaniciRepository.save(Kullanici.builder().isletme(isletme).kullaniciAdi("tmz-" + UUID.randomUUID())
                .sifre("x").adSoyad("Temizlik").rol(Rol.PERSONEL).durum(KullaniciDurum.AKTIF).build());
    }

    private RefreshToken token(Instant bitis, boolean iptal) {
        return refreshTokenRepository.save(RefreshToken.builder().kullanici(kullanici).token(UUID.randomUUID().toString())
                .expiryDate(bitis).revoked(iptal).build());
    }

    private List<OturumLog> olaylar() {
        return oturumLogRepository.findAll().stream().filter(l -> kullanici.getId().equals(l.getKullaniciId())).toList();
    }

    @Test
    void suresiDolanOturumBitisZamaniyla_yazilirVeBirDahaYazilmaz() {
        Instant simdi = Instant.now();
        Instant bitis = simdi.minus(Duration.ofDays(2));
        RefreshToken dolan = token(bitis, false);
        RefreshToken suruyor = token(simdi.plus(Duration.ofDays(3)), false);
        token(simdi.minus(Duration.ofDays(1)), true);

        assertTrue(temizlik.temizle(simdi).kapananOturum() >= 1);
        List<OturumLog> kayitlar = olaylar();
        assertEquals(1, kayitlar.size());
        assertEquals(OturumOlay.OTURUM_SURESI_DOLDU, kayitlar.get(0).getOlay());
        assertEquals(LocalDateTime.ofInstant(bitis, AppZaman.ISLETME), kayitlar.get(0).getZaman());
        assertTrue(refreshTokenRepository.findById(dolan.getId()).orElseThrow().isRevoked());
        assertFalse(refreshTokenRepository.findById(suruyor.getId()).orElseThrow().isRevoked());
        assertEquals(0, temizlik.temizle(simdi).kapananOturum());
        assertEquals(1, olaylar().size());
    }

    @Test
    void eskiTokenVeLoglarSilinir() {
        Instant simdi = Instant.now();
        RefreshToken eski = token(simdi.minus(Duration.ofDays(OturumTemizlikService.TOKEN_SAKLAMA_GUN + 1)), true);
        RefreshToken yeni = token(simdi.minus(Duration.ofDays(5)), true);
        oturumLogRepository.save(OturumLog.builder().kullaniciId(kullanici.getId()).kullaniciAdi("a").olay(OturumOlay.GIRIS)
                .zaman(AppZaman.simdi().minusDays(OturumTemizlikService.LOG_SAKLAMA_GUN + 1)).build());
        oturumLogRepository.save(OturumLog.builder().kullaniciId(kullanici.getId()).kullaniciAdi("a").olay(OturumOlay.GIRIS)
                .zaman(AppZaman.simdi().minusDays(10)).build());

        temizlik.temizle(simdi);
        refreshTokenRepository.flush();

        assertTrue(refreshTokenRepository.findById(eski.getId()).isEmpty());
        assertTrue(refreshTokenRepository.findById(yeni.getId()).isPresent());
        assertEquals(1, olaylar().size());
    }
}
