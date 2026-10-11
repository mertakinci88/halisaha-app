package com.msc.halisaha.service;

import com.msc.halisaha.TestOturum;
import com.msc.halisaha.common.AppZaman;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.dto.YoklamaKaydetRequest;
import com.msc.halisaha.dto.YoklamaKaydetRequest.Kayit;
import com.msc.halisaha.dto.YoklamaSatir;
import com.msc.halisaha.entity.Grup;
import com.msc.halisaha.entity.Isletme;
import com.msc.halisaha.entity.Kullanici;
import com.msc.halisaha.entity.Ogrenci;
import com.msc.halisaha.entity.OgrenciDurum;
import com.msc.halisaha.entity.Rol;
import com.msc.halisaha.entity.YoklamaDurum;
import com.msc.halisaha.repository.GrupRepository;
import com.msc.halisaha.repository.IsletmeRepository;
import com.msc.halisaha.repository.KullaniciRepository;
import com.msc.halisaha.repository.OgrenciRepository;
import com.msc.halisaha.repository.YoklamaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Her test geri alınan bir transaction'da çalışır; veritabanında kalıcı veri bırakmaz. */
@SpringBootTest
@Transactional
class YoklamaServiceTest {

    private static final LocalDate GUN = LocalDate.of(2026, 10, 5);

    @Autowired YoklamaService yoklamaService;
    @Autowired YoklamaRepository yoklamaRepository;
    @Autowired OgrenciRepository ogrenciRepository;
    @Autowired GrupRepository grupRepository;
    @Autowired IsletmeRepository isletmeRepository;
    @Autowired KullaniciRepository kullaniciRepository;
    @PersistenceContext EntityManager entityManager;

    private Kullanici a;
    private Kullanici b;
    private Grup grupA;
    private Ogrenci cigdem;
    private Ogrenci ali;
    private Ogrenci sule;
    private Ogrenci pasif;
    private long onceki;   // testten önceki yoklama sayısı (veritabanında başka kayıtlar olabilir)

    @BeforeEach
    void hazirla() {
        Isletme isletmeA = TestOturum.isletme(isletmeRepository, "A İşletmesi");
        Isletme isletmeB = TestOturum.isletme(isletmeRepository, "B İşletmesi");
        a = TestOturum.kullanici(kullaniciRepository, isletmeA, Rol.ADMIN);
        b = TestOturum.kullanici(kullaniciRepository, isletmeB, Rol.ADMIN);
        grupA = grupRepository.save(Grup.builder().isletme(isletmeA).ad("Minikler").build());
        cigdem = ogrenci(isletmeA, grupA, "Çiğdem Kaya", OgrenciDurum.AKTIF);
        ali = ogrenci(isletmeA, grupA, "Ali Demir", OgrenciDurum.AKTIF);
        sule = ogrenci(isletmeA, grupA, "Şule Aydın", OgrenciDurum.AKTIF);
        pasif = ogrenci(isletmeA, grupA, "Pasif Öğrenci", OgrenciDurum.PASIF);
        TestOturum.girisYap(a);
        onceki = yoklamaRepository.count();
    }

    @AfterEach
    void temizle() {
        TestOturum.temizle();
    }

    private Ogrenci ogrenci(Isletme isletme, Grup grup, String ad, OgrenciDurum durum) {
        return ogrenciRepository.save(Ogrenci.builder().isletme(isletme).grup(grup).adSoyad(ad).durum(durum).build());
    }

    private YoklamaKaydetRequest istek(Kayit... kayitlar) {
        return new YoklamaKaydetRequest(List.of(kayitlar));
    }

    private ApiException hata(Runnable is) {
        return assertThrows(ApiException.class, is::run);
    }

    @Test
    void listeAktifOgrencilerleAlfabetikGelirVeBaslangictaIsaretsizdir() {
        List<YoklamaSatir> liste = yoklamaService.listele(GUN, grupA.getId());
        assertEquals(List.of("Ali Demir", "Çiğdem Kaya", "Şule Aydın"), liste.stream().map(YoklamaSatir::adSoyad).toList());
        liste.forEach(s -> assertNull(s.durum()));
    }

    @Test
    void yoklamaKaydedilirVeAyniGunTekrarAcilincaGelir() {
        yoklamaService.kaydet(GUN, grupA.getId(), istek(
                new Kayit(ali.getId(), YoklamaDurum.GELDI, null),
                new Kayit(cigdem.getId(), YoklamaDurum.GELMEDI, "  Haber vermedi  "),
                new Kayit(sule.getId(), null, null)));

        List<YoklamaSatir> liste = yoklamaService.listele(GUN, grupA.getId());
        assertEquals(YoklamaDurum.GELDI, liste.get(0).durum());
        assertNull(liste.get(0).not());
        assertEquals(YoklamaDurum.GELMEDI, liste.get(1).durum());
        assertEquals("Haber vermedi", liste.get(1).not());
        assertNull(liste.get(2).durum());

        // Başka bir günde kayıt görünmez
        assertNull(yoklamaService.listele(GUN.plusDays(1), grupA.getId()).get(0).durum());
        assertEquals(onceki + 2, yoklamaRepository.count());
    }

    @Test
    void tekrarKaydetmekGunceller_durumsuzGonderilenKayitSilinir() {
        yoklamaService.kaydet(GUN, grupA.getId(), istek(
                new Kayit(ali.getId(), YoklamaDurum.GELDI, null),
                new Kayit(cigdem.getId(), YoklamaDurum.GELMEDI, "not")));

        List<YoklamaSatir> liste = yoklamaService.kaydet(GUN, grupA.getId(), istek(
                new Kayit(ali.getId(), YoklamaDurum.IZINLI, "hasta"),
                new Kayit(cigdem.getId(), null, null)));

        assertEquals(YoklamaDurum.IZINLI, liste.get(0).durum());
        assertEquals("hasta", liste.get(0).not());
        assertNull(liste.get(1).durum());
        assertEquals(onceki + 1, yoklamaRepository.count());   // çiğdem'in kaydı silindi, ali güncellendi (yeni kayıt açılmadı)
    }

    @Test
    void durumsuzNotReddedilir() {
        ApiException e = hata(() -> yoklamaService.kaydet(GUN, grupA.getId(),
                istek(new Kayit(ali.getId(), null, "not var ama durum yok"))));
        assertEquals(400, e.getStatus().value());
        assertEquals(onceki + 0, yoklamaRepository.count());
    }

    @Test
    void gelecekTarihReddedilir() {
        ApiException e = hata(() -> yoklamaService.kaydet(AppZaman.bugun().plusDays(1), grupA.getId(),
                istek(new Kayit(ali.getId(), YoklamaDurum.GELDI, null))));
        assertEquals(400, e.getStatus().value());
        // Bugün serbest
        yoklamaService.kaydet(AppZaman.bugun(), grupA.getId(), istek(new Kayit(ali.getId(), YoklamaDurum.GELDI, null)));
    }

    @Test
    void pasifVeBaskaGruptakiOgrenciVeTekrarEdenOgrenciReddedilir() {
        Grup baska = grupRepository.save(Grup.builder().isletme(grupA.getIsletme()).ad("Diğer").build());
        Ogrenci digerGrup = ogrenci(grupA.getIsletme(), baska, "Başka Grup", OgrenciDurum.AKTIF);

        assertEquals(400, hata(() -> yoklamaService.kaydet(GUN, grupA.getId(),
                istek(new Kayit(pasif.getId(), YoklamaDurum.GELDI, null)))).getStatus().value());
        assertEquals(400, hata(() -> yoklamaService.kaydet(GUN, grupA.getId(),
                istek(new Kayit(digerGrup.getId(), YoklamaDurum.GELDI, null)))).getStatus().value());
        assertEquals(400, hata(() -> yoklamaService.kaydet(GUN, grupA.getId(), istek(
                new Kayit(ali.getId(), YoklamaDurum.GELDI, null), new Kayit(ali.getId(), YoklamaDurum.GELMEDI, null)))).getStatus().value());
        assertEquals(onceki + 0, yoklamaRepository.count());
    }

    @Test
    void baskaIsletmeGrubunaVeOgrencisineErisilemez() {
        yoklamaService.kaydet(GUN, grupA.getId(), istek(new Kayit(ali.getId(), YoklamaDurum.GELDI, null)));

        TestOturum.girisYap(b);
        assertEquals(404, hata(() -> yoklamaService.listele(GUN, grupA.getId())).getStatus().value());
        assertEquals(404, hata(() -> yoklamaService.kaydet(GUN, grupA.getId(),
                istek(new Kayit(ali.getId(), YoklamaDurum.GELMEDI, null)))).getStatus().value());

        TestOturum.girisYap(a);
        assertEquals(YoklamaDurum.GELDI, yoklamaService.listele(GUN, grupA.getId()).get(0).durum());
    }

    @Test
    void ogrenciSilinirseYoklamaKayitlariDaSilinir() {
        yoklamaService.kaydet(GUN, grupA.getId(), istek(new Kayit(ali.getId(), YoklamaDurum.GELDI, null)));
        assertEquals(onceki + 1, yoklamaRepository.count());

        // Gerçek silme ayrı bir istekte çalışır; yoklama kaydı o oturumda yüklü değildir
        entityManager.flush();
        entityManager.clear();
        ogrenciRepository.deleteById(ali.getId());
        ogrenciRepository.flush();
        assertEquals(onceki + 0, yoklamaRepository.count());
    }
}
