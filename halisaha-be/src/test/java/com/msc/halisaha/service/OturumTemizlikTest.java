package com.msc.halisaha.service;

import com.msc.halisaha.common.AppZaman;
import com.msc.halisaha.common.security.IstemciBilgisi;
import com.msc.halisaha.dto.RefreshRequest;
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
import java.time.temporal.ChronoUnit;
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
    @Autowired AuthService authService;

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
        Instant simdi = Instant.now().truncatedTo(ChronoUnit.MICROS); // veritabanı mikrosaniye tutar
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
        Instant simdi = Instant.now().truncatedTo(ChronoUnit.MICROS); // veritabanı mikrosaniye tutar
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

    private RefreshToken sinyalli(Instant bitis, Instant sonGorulme) {
        RefreshToken t = token(bitis, false);
        t.setSonGorulme(sonGorulme);
        return refreshTokenRepository.saveAndFlush(t);
    }

    @Test
    void sinyaliKesilenOturumSonGorulmeZamaniylaYazilirBirDahaYazilmaz() {
        Instant simdi = Instant.now().truncatedTo(ChronoUnit.MICROS);
        Instant son = simdi.minus(Duration.ofMinutes(OturumTemizlikService.SINYAL_ZAMAN_ASIMI_DAKIKA + 5));
        RefreshToken kesilen = sinyalli(simdi.plus(Duration.ofDays(3)), son);
        RefreshToken canli = sinyalli(simdi.plus(Duration.ofDays(3)), simdi.minus(Duration.ofMinutes(2)));
        RefreshToken eskiKayit = sinyalli(simdi.plus(Duration.ofDays(3)), null);   // sinyal özelliğinden önceki oturum

        assertEquals(1, temizlik.tarayiciKapananlar(simdi));
        List<OturumLog> kayitlar = olaylar();
        assertEquals(1, kayitlar.size());
        assertEquals(OturumOlay.TARAYICI_KAPATILDI, kayitlar.get(0).getOlay());
        assertEquals(LocalDateTime.ofInstant(son, AppZaman.ISLETME), kayitlar.get(0).getZaman());
        assertTrue(refreshTokenRepository.findById(kesilen.getId()).orElseThrow().isKapanisYazildi());
        assertFalse(refreshTokenRepository.findById(kesilen.getId()).orElseThrow().isRevoked());   // oturum kapanmaz
        assertFalse(refreshTokenRepository.findById(canli.getId()).orElseThrow().isKapanisYazildi());
        assertFalse(refreshTokenRepository.findById(eskiKayit.getId()).orElseThrow().isKapanisYazildi());
        assertEquals(0, temizlik.tarayiciKapananlar(simdi));
    }

    @Test
    void sinyalGelinceOturumaDonduYazilirSonraSuresiDolunca_ikinciSonYazilmaz() {
        Instant simdi = Instant.now().truncatedTo(ChronoUnit.MICROS);
        RefreshToken t = sinyalli(simdi.plus(Duration.ofDays(3)), simdi.minus(Duration.ofMinutes(30)));
        temizlik.tarayiciKapananlar(simdi);

        authService.aktif(new RefreshRequest(t.getToken()), IstemciBilgisi.BILINMIYOR);
        assertEquals(List.of(OturumOlay.TARAYICI_KAPATILDI, OturumOlay.OTURUMA_DONDU),
                olaylar().stream().map(OturumLog::getOlay).sorted().toList());
        assertFalse(refreshTokenRepository.findById(t.getId()).orElseThrow().isKapanisYazildi());

        // tekrar sinyal: yeni kayıt yazılmaz
        authService.aktif(new RefreshRequest(t.getToken()), IstemciBilgisi.BILINMIYOR);
        assertEquals(2, olaylar().size());
    }

    @Test
    void kapanisYazilmisOturumunSuresiDolarsaAyriKayitYazilmaz() {
        Instant simdi = Instant.now().truncatedTo(ChronoUnit.MICROS);
        RefreshToken t = sinyalli(simdi.minus(Duration.ofHours(1)), simdi.minus(Duration.ofDays(2)));
        t.setKapanisYazildi(true);
        refreshTokenRepository.saveAndFlush(t);

        temizlik.temizle(simdi);
        assertEquals(0, olaylar().size());
        assertTrue(refreshTokenRepository.findById(t.getId()).orElseThrow().isRevoked());
    }
}
