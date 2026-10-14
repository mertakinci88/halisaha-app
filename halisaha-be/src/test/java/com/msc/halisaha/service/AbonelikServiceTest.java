package com.msc.halisaha.service;

import com.msc.halisaha.TestOturum;
import com.msc.halisaha.common.AppZaman;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.dto.AbonelikOdemeRequest;
import com.msc.halisaha.dto.AbonelikOnizlemeRequest;
import com.msc.halisaha.dto.AbonelikRequest;
import com.msc.halisaha.dto.AbonelikResponse;
import com.msc.halisaha.dto.RezervasyonRequest;
import com.msc.halisaha.entity.AbonelikDurum;
import com.msc.halisaha.entity.Isletme;
import com.msc.halisaha.entity.Kullanici;
import com.msc.halisaha.entity.OdemeDurumu;
import com.msc.halisaha.entity.OdemeYontemi;
import com.msc.halisaha.entity.Rol;
import com.msc.halisaha.entity.Rezervasyon;
import com.msc.halisaha.entity.RezervasyonDurum;
import com.msc.halisaha.entity.Saha;
import com.msc.halisaha.entity.SahaDurum;
import com.msc.halisaha.repository.IsletmeRepository;
import com.msc.halisaha.repository.KullaniciRepository;
import com.msc.halisaha.repository.RezervasyonKalemRepository;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class AbonelikServiceTest {

    @Autowired AbonelikService abonelikService;
    @Autowired RezervasyonService rezervasyonService;
    @Autowired IsletmeRepository isletmeRepository;
    @Autowired KullaniciRepository kullaniciRepository;
    @Autowired SahaRepository sahaRepository;
    @Autowired RezervasyonRepository rezervasyonRepository;
    @Autowired RezervasyonKalemRepository kalemRepository;

    private Kullanici a;
    private Kullanici b;
    private Saha saha;
    private LocalDate baslangic;

    @BeforeEach
    void hazirla() {
        Isletme isletmeA = TestOturum.isletme(isletmeRepository, "A İşletmesi");
        Isletme isletmeB = TestOturum.isletme(isletmeRepository, "B İşletmesi");
        a = TestOturum.kullanici(kullaniciRepository, isletmeA, Rol.ADMIN);
        b = TestOturum.kullanici(kullaniciRepository, isletmeB, Rol.ADMIN);
        saha = sahaRepository.save(Saha.builder().isletme(isletmeA).ad("Saha 1").durum(SahaDurum.AKTIF)
                .saatlikUcret(new BigDecimal("1000")).build());
        TestOturum.girisYap(a);
        baslangic = AppZaman.bugun().plusDays(3);
    }

    @AfterEach
    void temizle() {
        TestOturum.temizle();
    }

    private AbonelikRequest istek(String ucret, String odenen) {
        return new AbonelikRequest(saha.getId(), "Ali Abone", "05469764613", baslangic.getDayOfWeek().getValue(), 1140, 60,
                baslangic, new BigDecimal(ucret), new BigDecimal(odenen), OdemeYontemi.NAKIT, null, null, null);
    }

    private ApiException hata(Runnable is) {
        return assertThrows(ApiException.class, is::run);
    }

    @Test
    void abonelik52HaftalikRezervasyonOlusturur() {
        AbonelikResponse r = abonelikService.olustur(istek("10000", "4000"));

        assertEquals(baslangic.plusWeeks(51), r.bitisTarih());
        assertEquals(AbonelikDurum.AKTIF, r.durum());
        assertEquals(0, new BigDecimal("6000").compareTo(r.kalanTutar()));
        assertEquals(OdemeDurumu.KISMI_ODENDI, r.odemeDurumu());
        assertEquals(1, r.odemeler().size());

        // Her hafta bir rezervasyon: tam 52, ilki başlangıç günü, haftalık aralıklı, saat 19:00
        List<AbonelikResponse.HaftaResponse> haftalar = r.haftalar();
        assertEquals(52, haftalar.size());
        assertEquals(baslangic.atTime(19, 0), haftalar.get(0).baslangicTarih());
        assertEquals(baslangic.atTime(20, 0), haftalar.get(0).bitisTarih());
        assertEquals(baslangic.plusWeeks(1).atTime(19, 0), haftalar.get(1).baslangicTarih());
        assertEquals(r.bitisTarih(), haftalar.get(haftalar.size() - 1).baslangicTarih().toLocalDate());

        // Haftalık rezervasyonlar normal rezervasyon gibi saha takviminde görünür; saha ücreti 0, abonelikle ilişkili
        Rezervasyon ilk = rezervasyonRepository.findById(haftalar.get(0).rezervasyonId()).orElseThrow();
        assertEquals("Ali Abone", ilk.getAdSoyad());
        assertEquals(r.id(), ilk.getAbonelik().getId());
        assertEquals(0, BigDecimal.ZERO.compareTo(ilk.getOdenecekTutar()));
        assertEquals(1, kalemRepository.findByRezervasyonIdOrderByIdAsc(ilk.getId()).size());
    }

    @Test
    void geceSaatindekiAbonelikErtesiGuneYazilir() {
        AbonelikRequest istek = new AbonelikRequest(saha.getId(), "Gece Abone", "05469764613", baslangic.getDayOfWeek().getValue(), 1440, 120,
                baslangic, new BigDecimal("1000"), new BigDecimal("1000"), OdemeYontemi.KART, null, null, null);
        AbonelikResponse r = abonelikService.olustur(istek);
        // 24 = ertesi gün 00:00; iki saat sürer (02:00'de kapanır)
        assertEquals(baslangic.plusDays(1).atTime(0, 0), r.haftalar().get(0).baslangicTarih());
        assertEquals(baslangic.plusDays(1).atTime(2, 0), r.haftalar().get(0).bitisTarih());
        assertEquals(OdemeDurumu.ODENDI, r.odemeDurumu());
    }

    @Test
    void ucretOdemeSaatVeSahaDogrulanir() {
        assertEquals(400, hata(() -> abonelikService.olustur(istek("1000", "1000.01"))).getStatus().value());
        AbonelikRequest kapanisiAsan = new AbonelikRequest(saha.getId(), "Ali", "05469764613", 1, 1500, 120,
                baslangic, new BigDecimal("1000"), new BigDecimal("1000"), OdemeYontemi.NAKIT, null, null, null);
        assertEquals(400, hata(() -> abonelikService.olustur(kapanisiAsan)).getStatus().value());
        AbonelikRequest havaleHesapsiz = new AbonelikRequest(saha.getId(), "Ali", "05469764613", 1, 1140, 60,
                baslangic, new BigDecimal("1000"), new BigDecimal("500"), OdemeYontemi.HAVALE_EFT, null, null, null);
        assertEquals(400, hata(() -> abonelikService.olustur(havaleHesapsiz)).getStatus().value());
        AbonelikRequest gecmis = new AbonelikRequest(saha.getId(), "Ali", "05469764613", 1, 1140, 60,
                AppZaman.bugun().minusDays(1), new BigDecimal("1000"), new BigDecimal("500"), OdemeYontemi.NAKIT, null, null, null);
        assertEquals(400, hata(() -> abonelikService.olustur(gecmis)).getStatus().value());
    }

    @Test
    void cakisanRezervasyonVarsaAbonelikOlusmazVeOnizlemedeGorunur() {
        // Aboneliğin 3. haftasına denk gelen aktif bir rezervasyon
        LocalDateTime cakisan = baslangic.plusWeeks(2).atTime(19, 0);
        rezervasyonService.create(new RezervasyonRequest(saha.getId(), "Mehmet", cakisan, cakisan.plusHours(1),
                "05321234567", null, null, null, null));

        var onizleme = abonelikService.onizle(new AbonelikOnizlemeRequest(saha.getId(),
                baslangic.getDayOfWeek().getValue(), 1140, 60, baslangic));
        assertEquals(1, onizleme.cakismalar().size());
        assertEquals(baslangic.plusWeeks(2), onizleme.cakismalar().get(0).tarih());
        assertEquals("Mehmet", onizleme.cakismalar().get(0).adSoyad());

        ApiException e = hata(() -> abonelikService.olustur(istek("10000", "1000")));
        assertEquals(409, e.getStatus().value());
        assertTrue(e.getMessage().contains("Mehmet"));
        assertEquals(0, abonelikService.listele(null, null).size());      // hiçbir şey oluşmadı
    }

    @Test
    void iptalGelecekHaftalikRezervasyonlariIptalEderOdemelerKalir() {
        AbonelikResponse r = abonelikService.olustur(istek("10000", "4000"));
        AbonelikResponse iptal = abonelikService.iptalEt(r.id());

        assertEquals(AbonelikDurum.IPTAL, iptal.durum());
        assertTrue(iptal.haftalar().stream().allMatch(h -> h.durum() == RezervasyonDurum.IPTAL));
        assertEquals(1, iptal.odemeler().size());
        assertEquals(0, new BigDecimal("4000").compareTo(iptal.odenenTutar()));
        assertEquals(1, abonelikService.listele(AbonelikDurum.IPTAL, null).size());
        assertEquals(0, abonelikService.listele(AbonelikDurum.AKTIF, null).size());

        // İptal sonrası aynı saat yeniden abonelik açılabilir (rezervasyonlar boşaldı)
        assertEquals(AbonelikDurum.AKTIF, abonelikService.olustur(istek("10000", "4000")).durum());
    }

    @Test
    void ekOdemeKalaniAsamazSonOdemeSilinemez() {
        AbonelikResponse r = abonelikService.olustur(istek("10000", "4000"));
        assertEquals(400, hata(() -> abonelikService.odemeEkle(r.id(),
                new AbonelikOdemeRequest(new BigDecimal("6000.01"), OdemeYontemi.KART, null, null))).getStatus().value());

        AbonelikResponse tam = abonelikService.odemeEkle(r.id(), new AbonelikOdemeRequest(new BigDecimal("6000"), OdemeYontemi.KART, null, null));
        assertEquals(OdemeDurumu.ODENDI, tam.odemeDurumu());
        assertEquals(2, tam.odemeler().size());

        AbonelikResponse tek = abonelikService.odemeSil(r.id(), tam.odemeler().get(1).id());
        assertEquals(1, tek.odemeler().size());
        assertEquals(0, new BigDecimal("6000").compareTo(tek.kalanTutar()));
        assertEquals(400, hata(() -> abonelikService.odemeSil(r.id(), tek.odemeler().get(0).id())).getStatus().value());
        assertEquals(400, hata(() -> abonelikService.odemeEkle(r.id(),
                new AbonelikOdemeRequest(new BigDecimal("1"), OdemeYontemi.NAKIT, null, AppZaman.bugun().plusDays(1)))).getStatus().value());
    }

    @Test
    void guncellemedeUcretOdemeninAltinaDusmezAdTelefonGelecekHaftalaraYansir() {
        AbonelikResponse r = abonelikService.olustur(istek("10000", "4000"));
        assertEquals(400, hata(() -> abonelikService.guncelle(r.id(),
                new com.msc.halisaha.dto.AbonelikGuncelleRequest("Ali", "05469764613", new BigDecimal("3999"), null))).getStatus().value());

        AbonelikResponse g = abonelikService.guncelle(r.id(),
                new com.msc.halisaha.dto.AbonelikGuncelleRequest("Veli Abone", "05001112233", new BigDecimal("12000"), "Kış ayları cuma"));
        assertEquals("Veli Abone", g.adSoyad());
        assertEquals(0, new BigDecimal("8000").compareTo(g.kalanTutar()));
        Rezervasyon son = rezervasyonRepository.findById(g.haftalar().get(5).rezervasyonId()).orElseThrow();
        assertEquals("Veli Abone", son.getAdSoyad());
        assertEquals("05001112233", son.getTelNo());
    }

    @Test
    void standartUcretKaydedilirVeIsletmelerAyrisir() {
        assertEquals(null, abonelikService.ayar().standartUcret());
        abonelikService.ayarKaydet(new com.msc.halisaha.dto.AbonelikAyarRequest(new BigDecimal("15000")));
        assertEquals(0, new BigDecimal("15000").compareTo(abonelikService.ayar().standartUcret()));
        abonelikService.ayarKaydet(new com.msc.halisaha.dto.AbonelikAyarRequest(new BigDecimal("16000")));
        assertEquals(0, new BigDecimal("16000").compareTo(abonelikService.ayar().standartUcret()));

        TestOturum.girisYap(b);
        assertEquals(null, abonelikService.ayar().standartUcret());
    }

    @Test
    void baskaIsletmeninAboneligineErisilemez() {
        AbonelikResponse r = abonelikService.olustur(istek("10000", "4000"));
        TestOturum.girisYap(b);
        assertEquals(404, hata(() -> abonelikService.getir(r.id())).getStatus().value());
        assertEquals(404, hata(() -> abonelikService.iptalEt(r.id())).getStatus().value());
        assertEquals(0, abonelikService.listele(null, null).size());
        // başka işletmenin sahasıyla abonelik açılamaz
        assertEquals(404, hata(() -> abonelikService.olustur(istek("1000", "500"))).getStatus().value());
    }

    @Test
    void buckluSaatliAbonelikYarimSaatOlarakYazilirVeCakismaYarimSaattenHesaplanir() {
        int gun = baslangic.getDayOfWeek().getValue();
        // 20:30 - 21:30 abonelik
        AbonelikResponse r = abonelikService.olustur(new AbonelikRequest(saha.getId(), "Buçuklu Abone", "05469764613", gun, 1230, 60,
                baslangic, new BigDecimal("10000"), new BigDecimal("10000"), OdemeYontemi.NAKIT, null, null, null));
        assertEquals(1230, r.baslangicDakika());
        assertEquals(60, r.sureDakika());
        assertEquals(baslangic.atTime(20, 30), r.haftalar().get(0).baslangicTarih());
        assertEquals(baslangic.atTime(21, 30), r.haftalar().get(0).bitisTarih());

        // 20:00-21:00 ile çakışır (20:30-21:00 ortak), 21:30-22:30 ile çakışmaz
        var cakisan = abonelikService.onizle(new AbonelikOnizlemeRequest(saha.getId(), gun, 1200, 60, baslangic));
        assertEquals(r.haftalar().size(), cakisan.cakismalar().size());
        var bitisik = abonelikService.onizle(new AbonelikOnizlemeRequest(saha.getId(), gun, 1290, 60, baslangic));
        assertEquals(0, bitisik.cakismalar().size());

        // 30'un katı olmayan dakika kabul edilmez
        assertEquals(400, hata(() -> abonelikService.olustur(new AbonelikRequest(saha.getId(), "Ali", "05469764613", gun, 1215, 60,
                baslangic, new BigDecimal("1000"), new BigDecimal("500"), OdemeYontemi.NAKIT, null, null, null))).getStatus().value());
        // Kapanışı (02:00) aşan buçuklu seçim: 01:30 + 1 saat
        assertEquals(400, hata(() -> abonelikService.olustur(new AbonelikRequest(saha.getId(), "Ali", "05469764613", gun, 1530, 60,
                baslangic, new BigDecimal("1000"), new BigDecimal("500"), OdemeYontemi.NAKIT, null, null, null))).getStatus().value());
    }
}
