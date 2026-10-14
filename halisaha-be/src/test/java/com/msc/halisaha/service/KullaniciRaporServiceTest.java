package com.msc.halisaha.service;

import com.msc.halisaha.TestOturum;
import com.msc.halisaha.common.AppZaman;
import com.msc.halisaha.dto.IadeRequest;
import com.msc.halisaha.dto.KullaniciRaporlari;
import com.msc.halisaha.dto.RezervasyonRequest;
import com.msc.halisaha.dto.RezervasyonResponse;
import com.msc.halisaha.entity.Hesap;
import com.msc.halisaha.entity.HesapDurum;
import com.msc.halisaha.entity.Isletme;
import com.msc.halisaha.entity.Kullanici;
import com.msc.halisaha.entity.OdemeYontemi;
import com.msc.halisaha.entity.Rol;
import com.msc.halisaha.entity.Saha;
import com.msc.halisaha.entity.SahaDurum;
import com.msc.halisaha.repository.HesapRepository;
import com.msc.halisaha.repository.IadeRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class KullaniciRaporServiceTest {

    @Autowired KullaniciRaporService raporService;
    @Autowired RezervasyonService rezervasyonService;
    @Autowired IadeService iadeService;
    @Autowired IadeRepository iadeRepository;
    @Autowired IsletmeRepository isletmeRepository;
    @Autowired KullaniciRepository kullaniciRepository;
    @Autowired SahaRepository sahaRepository;
    @Autowired HesapRepository hesapRepository;

    private Kullanici admin;
    private Kullanici personel;
    private Saha saha;
    private LocalDate bugun;
    /** Ödemeler işletme gününe göre yazılır (00:00-02:00 arası bir önceki gün); iade ise takvim gününe. Kasa aralığı ikisini de kapsar. */
    private LocalDate kasaBaslangic;

    @BeforeEach
    void hazirla() {
        Isletme isletme = TestOturum.isletme(isletmeRepository, "A İşletmesi");
        admin = TestOturum.kullanici(kullaniciRepository, isletme, Rol.ADMIN);
        personel = TestOturum.kullanici(kullaniciRepository, isletme, Rol.PERSONEL);
        admin.setAdSoyad("Yönetici Kişi");
        personel.setAdSoyad("Personel Kişi");
        saha = sahaRepository.save(Saha.builder().isletme(isletme).ad("Saha 1").durum(SahaDurum.AKTIF)
                .saatlikUcret(new BigDecimal("1000")).build());
        bugun = AppZaman.bugun();
        kasaBaslangic = AppZaman.simdi().minusHours(2).toLocalDate();
    }

    @AfterEach
    void temizle() {
        TestOturum.temizle();
    }

    private RezervasyonResponse rezervasyon(Kullanici yapan, int gun, String kapora, OdemeYontemi yontem) {
        TestOturum.girisYap(yapan);
        LocalDateTime baslangic = bugun.plusDays(gun).atTime(19, 0);
        return rezervasyonService.create(new RezervasyonRequest(saha.getId(), "Müşteri " + gun, baslangic, baslangic.plusHours(1),
                "05469764613", yontem, new BigDecimal(kapora), null, false));
    }

    private KullaniciRaporlari.KasaSatiri satir(KullaniciRaporlari.Kasa kasa, Kullanici k) {
        return kasa.satirlar().stream().filter(s -> k.getId().equals(s.kullaniciId())).findFirst().orElseThrow();
    }

    @Test
    void kasaTahsilatiAlanKullaniciyaYazarIadeniIadeEdeneDusurur() {
        RezervasyonResponse r1 = rezervasyon(personel, 3, "1000", OdemeYontemi.NAKIT);
        rezervasyon(admin, 4, "500", OdemeYontemi.KART);
        TestOturum.girisYap(admin);
        iadeService.yap(new IadeRequest(r1.id(), null, null, null, new BigDecimal("200"), OdemeYontemi.NAKIT, null, null, "Fazla alındı"));
        iadeRepository.flush();

        var kasa = raporService.kasa(kasaBaslangic, bugun);
        var p = satir(kasa, personel);
        assertEquals(0, new BigDecimal("1000").compareTo(p.rezervasyon()));
        assertEquals(0, new BigDecimal("1000").compareTo(p.nakit()));
        assertEquals(0, BigDecimal.ZERO.compareTo(p.iade()));
        var a = satir(kasa, admin);
        assertEquals(0, new BigDecimal("500").compareTo(a.kart()));
        assertEquals(0, new BigDecimal("200").compareTo(a.iade()));
        assertEquals(0, new BigDecimal("300").compareTo(a.net()));
        assertEquals(0, new BigDecimal("-200").compareTo(a.netNakit()));      // nakit kasadan 200 çıktı
        // Toplam satırı: 1500 tahsilat - 200 iade
        assertEquals(0, new BigDecimal("1500").compareTo(kasa.toplam().tahsilat()));
        assertEquals(0, new BigDecimal("1300").compareTo(kasa.toplam().net()));
    }

    @Test
    void islemSayilariVeIptalEdenKaydedilir() {
        RezervasyonResponse r1 = rezervasyon(personel, 3, "0", OdemeYontemi.NAKIT);
        rezervasyon(personel, 4, "0", OdemeYontemi.NAKIT);
        rezervasyon(admin, 5, "0", OdemeYontemi.NAKIT);
        TestOturum.girisYap(personel);
        RezervasyonResponse iptal = rezervasyonService.cancel(r1.id(), "Müşteri vazgeçti");
        assertEquals("Personel Kişi", iptal.iptalEdenAd());
        assertEquals("Müşteri vazgeçti", iptal.iptalNeden());
        assertNotNull(iptal.iptalTarih());
        kullaniciRepository.flush();

        TestOturum.girisYap(admin);
        var islemler = raporService.islemler(bugun, bugun);
        var p = islemler.satirlar().stream().filter(s -> personel.getId().equals(s.kullaniciId())).findFirst().orElseThrow();
        assertEquals(2, p.rezervasyon());
        assertEquals(1, p.rezervasyonIptal());
        var a = islemler.satirlar().stream().filter(s -> admin.getId().equals(s.kullaniciId())).findFirst().orElseThrow();
        assertEquals(1, a.rezervasyon());
        assertEquals(0, a.rezervasyonIptal());

        var iptaller = raporService.iptaller(bugun, bugun);
        assertEquals(1, iptaller.toplam());
        assertEquals("Personel Kişi", iptaller.satirlar().get(0).iptalEden());
        assertEquals("Müşteri vazgeçti", iptaller.satirlar().get(0).neden());
        assertEquals(1, iptaller.kisiler().stream().filter(k -> personel.getId().equals(k.kullaniciId())).findFirst().orElseThrow().rezervasyon());
    }

    @Test
    void iadeRaporuToplamKaynakVeKullaniciyiVerir() {
        RezervasyonResponse r1 = rezervasyon(personel, 3, "1000", OdemeYontemi.NAKIT);
        TestOturum.girisYap(admin);
        iadeService.yap(new IadeRequest(r1.id(), null, null, null, new BigDecimal("300"), OdemeYontemi.KART, null, null, "Hatalı tahsilat"));
        iadeRepository.flush();

        var iadeler = raporService.iadeler(bugun, bugun);
        assertEquals(0, new BigDecimal("300").compareTo(iadeler.toplam()));
        assertEquals("REZERVASYON", iadeler.kaynaklar().get(0).kaynak());
        assertEquals(1, iadeler.satirlar().size());
        assertEquals("Yönetici Kişi", iadeler.satirlar().get(0).kullanici());
        assertEquals("Müşteri 3", iadeler.satirlar().get(0).ilgili());
        assertEquals("Hatalı tahsilat", iadeler.satirlar().get(0).neden());
    }

    @Test
    void yoklamaVeGirisRaporlariBosDonenIsletmedeHataVermez() {
        TestOturum.girisYap(admin);
        var yoklama = raporService.yoklama(bugun, bugun);
        assertTrue(yoklama.gunler().isEmpty());
        var giris = raporService.giris(bugun, bugun, null);
        assertTrue(giris.satirlar().isEmpty());
    }

    @Test
    void havaleTahsilatiHangiHesabaYattigiylaGosterilir() {
        Hesap ziraat = hesapRepository.save(Hesap.builder().isletme(admin.getIsletme()).ad("Ziraat Bankası")
                .iban("TR000000000000000000000001").hesapSahibi("İşletme").durum(HesapDurum.AKTIF).kayitTarih(LocalDateTime.now()).build());
        Hesap garanti = hesapRepository.save(Hesap.builder().isletme(admin.getIsletme()).ad("Garanti")
                .iban("TR000000000000000000000002").hesapSahibi("İşletme").durum(HesapDurum.AKTIF).kayitTarih(LocalDateTime.now()).build());
        TestOturum.girisYap(personel);
        LocalDateTime b1 = bugun.plusDays(3).atTime(19, 0);
        rezervasyonService.create(new RezervasyonRequest(saha.getId(), "Havale 1", b1, b1.plusHours(1), "05469764613",
                OdemeYontemi.HAVALE_EFT, new BigDecimal("700"), ziraat.getId(), false));
        LocalDateTime b2 = bugun.plusDays(4).atTime(19, 0);
        rezervasyonService.create(new RezervasyonRequest(saha.getId(), "Havale 2", b2, b2.plusHours(1), "05469764613",
                OdemeYontemi.HAVALE_EFT, new BigDecimal("300"), garanti.getId(), false));
        LocalDateTime b3 = bugun.plusDays(5).atTime(19, 0);
        rezervasyonService.create(new RezervasyonRequest(saha.getId(), "Havale 3", b3, b3.plusHours(1), "05469764613",
                OdemeYontemi.HAVALE_EFT, new BigDecimal("200"), ziraat.getId(), false));

        var kasa = raporService.kasa(kasaBaslangic, bugun);
        var p = satir(kasa, personel);
        assertEquals(0, new BigDecimal("1200").compareTo(p.havale()));
        // Hesaplar tutara göre büyükten küçüğe: Ziraat 900, Garanti 300
        assertEquals(2, p.havaleHesaplari().size());
        assertEquals("Ziraat Bankası", p.havaleHesaplari().get(0).hesapAd());
        assertEquals(0, new BigDecimal("900").compareTo(p.havaleHesaplari().get(0).tutar()));
        assertEquals("Garanti", p.havaleHesaplari().get(1).hesapAd());
        // Toplam satırı tüm kullanıcıların havalesini hesaba göre toplar
        assertEquals(2, kasa.toplam().havaleHesaplari().size());
        // Havale dışı ödeme yapan kullanıcının hesap listesi boştur
        assertTrue(satir(kasa, admin).havaleHesaplari().isEmpty());
    }
}
