package com.msc.halisaha.service;

import com.msc.halisaha.TestOturum;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.dto.GrupRequest;
import com.msc.halisaha.dto.IsletmeOlusturRequest;
import com.msc.halisaha.dto.KullaniciOlusturRequest;
import com.msc.halisaha.dto.RezervasyonRequest;
import com.msc.halisaha.dto.SahaRequest;
import com.msc.halisaha.dto.UrunRequest;
import com.msc.halisaha.dto.RezervasyonUrunRequest;
import com.msc.halisaha.entity.Isletme;
import com.msc.halisaha.entity.IsletmeDurum;
import com.msc.halisaha.entity.Kullanici;
import com.msc.halisaha.entity.Rol;
import com.msc.halisaha.repository.IsletmeRepository;
import com.msc.halisaha.repository.KullaniciRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** İki işletme arasında veri sızıntısı olmadığını doğrular. Her test geri alınan bir transaction'da çalışır. */
@SpringBootTest
@Transactional
class IsletmeIzolasyonTest {

    @Autowired SahaService sahaService;
    @Autowired GrupService grupService;
    @Autowired UrunService urunService;
    @Autowired RezervasyonService rezervasyonService;
    @Autowired RaporService raporService;
    @Autowired KullaniciService kullaniciService;
    @Autowired IsletmeService isletmeService;
    @Autowired IsletmeRepository isletmeRepository;
    @Autowired KullaniciRepository kullaniciRepository;

    private Kullanici a;
    private Kullanici b;

    @BeforeEach
    void hazirla() {
        Isletme isletmeA = TestOturum.isletme(isletmeRepository, "A İşletmesi");
        Isletme isletmeB = TestOturum.isletme(isletmeRepository, "B İşletmesi");
        a = TestOturum.kullanici(kullaniciRepository, isletmeA, Rol.ADMIN);
        b = TestOturum.kullanici(kullaniciRepository, isletmeB, Rol.ADMIN);
    }

    @AfterEach
    void temizle() {
        TestOturum.temizle();
    }

    @Test
    void sahaVeUrunListeleriSadeceKendiIsletmesini_gosterir() {
        TestOturum.girisYap(a);
        Long sahaA = sahaService.create(new SahaRequest("A Sahası", null, new BigDecimal("500"))).id();
        Long urunA = urunService.create(new UrunRequest("A Ürünü", 5, new BigDecimal("20"), 10)).id();

        TestOturum.girisYap(b);
        assertFalse(sahaService.list().stream().anyMatch(s -> s.id().equals(sahaA)));
        assertFalse(urunService.list().stream().anyMatch(u -> u.id().equals(urunA)));

        TestOturum.girisYap(a);
        assertTrue(sahaService.list().stream().anyMatch(s -> s.id().equals(sahaA)));
        assertTrue(urunService.list().stream().anyMatch(u -> u.id().equals(urunA)));
    }

    @Test
    void baskaIsletmeninKaydinaIdIleErisilemez() {
        TestOturum.girisYap(a);
        Long sahaA = sahaService.create(new SahaRequest("A Sahası", null, new BigDecimal("500"))).id();
        Long grupA = grupService.create(new GrupRequest("A Grubu")).id();
        Long urunA = urunService.create(new UrunRequest("A Ürünü", 5, new BigDecimal("20"), 10)).id();

        TestOturum.girisYap(b);
        assertEquals(404, assertThrows(ApiException.class, () -> sahaService.getById(sahaA)).getStatus().value());
        assertEquals(404, assertThrows(ApiException.class, () -> grupService.getById(grupA)).getStatus().value());
        assertEquals(404, assertThrows(ApiException.class, () -> urunService.getById(urunA)).getStatus().value());
        assertThrows(ApiException.class, () -> sahaService.delete(sahaA));
        assertThrows(ApiException.class, () -> urunService.delete(urunA));
    }

    @Test
    void rezervasyonBaskaIsletmeninSahasiyleVeyaUrunuyleOlusturulamaz() {
        TestOturum.girisYap(a);
        Long sahaA = sahaService.create(new SahaRequest("A Sahası", null, new BigDecimal("500"))).id();
        Long urunA = urunService.create(new UrunRequest("A Ürünü", 5, new BigDecimal("20"), 10)).id();
        LocalDateTime bas = LocalDateTime.of(2031, 1, 5, 14, 0);

        TestOturum.girisYap(b);
        assertEquals(404, assertThrows(ApiException.class, () -> rezervasyonService.create(
                new RezervasyonRequest(sahaA, "Müşteri", bas, bas.plusHours(1), "05551112233", null, null, null, null)))
                .getStatus().value());

        // B kendi sahasında rezervasyon açar; A'nın ürününü ekleyemez ve A rezervasyonu göremez
        Long sahaB = sahaService.create(new SahaRequest("B Sahası", null, new BigDecimal("500"))).id();
        Long rezB = rezervasyonService.create(
                new RezervasyonRequest(sahaB, "Müşteri", bas, bas.plusHours(1), "05551112233", null, null, null, null)).id();
        assertEquals(404, assertThrows(ApiException.class,
                () -> rezervasyonService.urunEkle(rezB, new RezervasyonUrunRequest(urunA, 1))).getStatus().value());

        TestOturum.girisYap(a);
        assertEquals(404, assertThrows(ApiException.class, () -> rezervasyonService.getById(rezB)).getStatus().value());
        assertTrue(rezervasyonService.list(null, bas.toLocalDate()).isEmpty());
    }

    @Test
    void ayniSahadaFarkliIsletmelerinRezervasyonlariBirbirineEtkisizdir() {
        TestOturum.girisYap(a);
        Long sahaA = sahaService.create(new SahaRequest("Saha", null, new BigDecimal("500"))).id();
        LocalDateTime bas = LocalDateTime.of(2031, 2, 5, 14, 0);
        rezervasyonService.create(new RezervasyonRequest(sahaA, "A Müşteri", bas, bas.plusHours(1), "05551112233", null, null, null, null));

        TestOturum.girisYap(b);
        Long sahaB = sahaService.create(new SahaRequest("Saha", null, new BigDecimal("500"))).id();
        // Aynı saat diliminde B'nin çakışma hatası almaması gerekir
        rezervasyonService.create(new RezervasyonRequest(sahaB, "B Müşteri", bas, bas.plusHours(1), "05551112233", null, null, null, null));
        assertEquals(1, rezervasyonService.list(null, bas.toLocalDate()).size());
    }

    @Test
    void ciroRaporuSadeceKendiIsletmesiniKapsar() {
        TestOturum.girisYap(a);
        Long sahaA = sahaService.create(new SahaRequest("A Sahası", null, new BigDecimal("1000"))).id();
        LocalDateTime bas = LocalDateTime.of(2031, 3, 5, 14, 0);
        rezervasyonService.create(new RezervasyonRequest(sahaA, "A Müşteri", bas, bas.plusHours(1), "05551112233", null, null, null, null));
        LocalDate gun = bas.toLocalDate();

        var raporA = raporService.ciro(gun, gun, null, RaporService.Grupla.GUN, RaporService.Esas.SEANS);
        assertEquals(0, new BigDecimal("1000").compareTo(raporA.toplam()));

        TestOturum.girisYap(b);
        var raporB = raporService.ciro(gun, gun, null, RaporService.Grupla.GUN, RaporService.Esas.SEANS);
        assertEquals(0, BigDecimal.ZERO.compareTo(raporB.toplam()));
    }

    @Test
    void kullaniciYonetimiSadeceKendiIsletmesindedir() {
        TestOturum.girisYap(b);
        assertEquals(404, assertThrows(ApiException.class, () -> kullaniciService.getById(a.getId())).getStatus().value());
        assertFalse(kullaniciService.list().stream().anyMatch(k -> k.id().equals(a.getId())));
        assertThrows(ApiException.class, () -> kullaniciService.create(
                new KullaniciOlusturRequest("x-" + UUID.randomUUID(), "123456", "Ad", Rol.SUPER_ADMIN, null)));
    }

    @Test
    void adminSadeceKendiIsletmesindekiKullaniciyiGuncelleyebilir() {
        Kullanici personelA = TestOturum.kullanici(kullaniciRepository, a.getIsletme(), Rol.PERSONEL);

        TestOturum.girisYap(a);
        var guncel = kullaniciService.guncelle(personelA.getId(),
                new com.msc.halisaha.dto.KullaniciGuncelleRequest(personelA.getKullaniciAdi(), "  Yeni Ad  ", "yenisifre1"));
        assertEquals("Yeni Ad", guncel.adSoyad());
        assertTrue(kullaniciRepository.findById(personelA.getId()).orElseThrow().getSifre().startsWith("$2"));

        // Şifre boşsa değişmez
        String eskiHash = kullaniciRepository.findById(personelA.getId()).orElseThrow().getSifre();
        kullaniciService.guncelle(personelA.getId(), new com.msc.halisaha.dto.KullaniciGuncelleRequest(personelA.getKullaniciAdi(), "Başka Ad", null));
        assertEquals(eskiHash, kullaniciRepository.findById(personelA.getId()).orElseThrow().getSifre());

        // Başka işletmenin admini güncelleyemez
        TestOturum.girisYap(b);
        assertEquals(404, assertThrows(ApiException.class, () -> kullaniciService.guncelle(personelA.getId(),
                new com.msc.halisaha.dto.KullaniciGuncelleRequest("hacked-" + UUID.randomUUID(), "Hacklendi", "hacked123"))).getStatus().value());
        assertEquals("Başka Ad", kullaniciRepository.findById(personelA.getId()).orElseThrow().getAdSoyad());
        assertEquals(personelA.getKullaniciAdi(), kullaniciRepository.findById(personelA.getId()).orElseThrow().getKullaniciAdi());
    }

    @Test
    void kullaniciAdiDegisirAmaBaskasininkiyleCakisamaz() {
        Kullanici personelA = TestOturum.kullanici(kullaniciRepository, a.getIsletme(), Rol.PERSONEL);
        TestOturum.girisYap(a);

        String yeniAd = "yeni-" + UUID.randomUUID();
        var guncel = kullaniciService.guncelle(personelA.getId(),
                new com.msc.halisaha.dto.KullaniciGuncelleRequest("  " + yeniAd + "  ", "Ad", null));
        assertEquals(yeniAd, guncel.kullaniciAdi());

        // Başka işletmedeki bir kullanıcının adı da alınamaz (global benzersiz)
        assertEquals(409, assertThrows(ApiException.class, () -> kullaniciService.guncelle(personelA.getId(),
                new com.msc.halisaha.dto.KullaniciGuncelleRequest(b.getKullaniciAdi(), "Ad", null))).getStatus().value());
        // Kendi mevcut adını göndermek çakışma sayılmaz
        kullaniciService.guncelle(personelA.getId(), new com.msc.halisaha.dto.KullaniciGuncelleRequest(yeniAd, "Ad", null));
    }

    @Test
    void kullaniciVeGrupListesiAlfabetikVeDurumDegisinceSiraDegismez() {
        Kullanici k1 = TestOturum.kullanici(kullaniciRepository, a.getIsletme(), Rol.PERSONEL);
        Kullanici k2 = TestOturum.kullanici(kullaniciRepository, a.getIsletme(), Rol.PERSONEL);
        Kullanici k3 = TestOturum.kullanici(kullaniciRepository, a.getIsletme(), Rol.PERSONEL);
        k1.setAdSoyad("Çiğdem"); k2.setAdSoyad("Ali"); k3.setAdSoyad("Şule");
        a.setAdSoyad("Zeynep");
        kullaniciRepository.flush();

        TestOturum.girisYap(a);
        java.util.function.Supplier<java.util.List<String>> sira = () ->
                kullaniciService.list().stream().map(com.msc.halisaha.dto.KullaniciResponse::adSoyad).toList();
        var beklenen = java.util.List.of("Ali", "Çiğdem", "Şule", "Zeynep");
        assertEquals(beklenen, sira.get());

        // Ortadaki kullanıcıyı pasife alıp tekrar aktif et: sıra aynı kalmalı
        kullaniciService.durumGuncelle(k1.getId(), com.msc.halisaha.entity.KullaniciDurum.PASIF);
        kullaniciService.durumGuncelle(k1.getId(), com.msc.halisaha.entity.KullaniciDurum.AKTIF);
        assertEquals(beklenen, sira.get());

        grupService.create(new GrupRequest("Zeytin"));
        grupService.create(new GrupRequest("Çam"));
        grupService.create(new GrupRequest("Ata"));
        assertEquals(java.util.List.of("Ata", "Çam", "Zeytin"),
                grupService.list().stream().map(com.msc.halisaha.dto.GrupResponse::ad).toList());
    }

    @Test
    void sonAktifAdminPasifYapilamaz() {
        TestOturum.girisYap(a);
        assertThrows(ApiException.class,
                () -> kullaniciService.durumGuncelle(a.getId(), com.msc.halisaha.entity.KullaniciDurum.PASIF));
    }

    @Test
    void superAdminIsletmeVerisineErisemez() {
        Kullanici superAdmin = kullaniciRepository.save(Kullanici.builder()
                .kullaniciAdi("super-" + UUID.randomUUID()).sifre("x").adSoyad("Süper").rol(Rol.SUPER_ADMIN).build());
        TestOturum.girisYap(superAdmin);
        assertEquals(403, assertThrows(ApiException.class, () -> sahaService.list()).getStatus().value());
    }

    @Test
    void isletmeOlusturulurIlkAdminiylePasifIsletmeninKullanicisiGirisYapamaz() {
        var isletme = isletmeService.create(new IsletmeOlusturRequest("C İşletmesi", "C Sahibi", null,
                new IsletmeOlusturRequest.Yonetici("c-" + UUID.randomUUID(), "123456", "C Yönetici")));
        var kullanicilar = kullaniciService.list(isletme.id());
        assertEquals(1, kullanicilar.size());
        assertEquals(Rol.ADMIN, kullanicilar.get(0).rol());

        Kullanici yonetici = kullaniciRepository.findById(kullanicilar.get(0).id()).orElseThrow();
        assertTrue(new com.msc.halisaha.common.security.UserPrincipal(yonetici).isEnabled());
        isletmeService.durumGuncelle(isletme.id(), IsletmeDurum.PASIF);
        assertFalse(new com.msc.halisaha.common.security.UserPrincipal(yonetici).isEnabled());
    }
}
