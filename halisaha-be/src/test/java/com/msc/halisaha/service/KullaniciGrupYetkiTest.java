package com.msc.halisaha.service;

import com.msc.halisaha.TestOturum;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.dto.GrupYetkiGuncelleRequest;
import com.msc.halisaha.dto.KullaniciResponse;
import com.msc.halisaha.dto.OgrenciRequest;
import com.msc.halisaha.dto.OgrenciResponse;
import com.msc.halisaha.entity.Grup;
import com.msc.halisaha.entity.Isletme;
import com.msc.halisaha.entity.Kullanici;
import com.msc.halisaha.entity.Ogrenci;
import com.msc.halisaha.entity.Rol;
import com.msc.halisaha.repository.GrupRepository;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class KullaniciGrupYetkiTest {

    @Autowired KullaniciService kullaniciService;
    @Autowired OgrenciService ogrenciService;
    @Autowired GrupService grupService;
    @Autowired IsletmeRepository isletmeRepository;
    @Autowired KullaniciRepository kullaniciRepository;
    @Autowired GrupRepository grupRepository;
    @Autowired OgrenciRepository ogrenciRepository;

    private Isletme isletme;
    private Kullanici admin;
    private Kullanici personel;
    private Grup a;
    private Grup b;
    private Grup c;
    private Ogrenci ogrA;
    private Ogrenci ogrB;
    private Ogrenci ogrC;

    @BeforeEach
    void hazirla() {
        isletme = TestOturum.isletme(isletmeRepository, "Grup Yetki");
        admin = TestOturum.kullanici(kullaniciRepository, isletme, Rol.ADMIN);
        personel = TestOturum.kullanici(kullaniciRepository, isletme, Rol.PERSONEL);
        a = grupRepository.save(Grup.builder().isletme(isletme).ad("A Grubu").build());
        b = grupRepository.save(Grup.builder().isletme(isletme).ad("B Grubu").build());
        c = grupRepository.save(Grup.builder().isletme(isletme).ad("C Grubu").build());
        ogrA = ogrenci("Ali", a);
        ogrB = ogrenci("Burak", b);
        ogrC = ogrenci("Cem", c);
        TestOturum.girisYap(admin);
    }

    @AfterEach
    void temizle() {
        TestOturum.temizle();
    }

    private Ogrenci ogrenci(String ad, Grup grup) {
        return ogrenciRepository.save(Ogrenci.builder().isletme(isletme).grup(grup).adSoyad(ad).build());
    }

    private void personelOlarakGir(boolean kisitli, Long... gruplar) {
        personel.setGrupKisitli(kisitli);
        personel.getIzinliGruplar().clear();
        personel.getIzinliGruplar().addAll(List.of(gruplar));
        TestOturum.girisYap(personel);
    }

    private List<String> adlar() {
        return ogrenciService.list(null, null, false, false, 0, 50).icerik().stream()
                .map(OgrenciResponse::adSoyad).toList();
    }

    @Test
    void varsayilanOlarakHerGrupGorunur() {
        personelOlarakGir(false);
        assertEquals(List.of("Ali", "Burak", "Cem"), adlar());
        assertEquals(3, grupService.list().size());
    }

    @Test
    void kisitliPersonelSadeceSecilenGruplarinOgrencileriniGorur() {
        personelOlarakGir(true, a.getId(), c.getId());
        assertEquals(List.of("Ali", "Cem"), adlar());
        assertEquals(List.of("A Grubu", "C Grubu"), grupService.list().stream().map(g -> g.ad()).toList());
    }

    @Test
    void transferHedefListesiGrupYetkisindenBagimsizdirTumGruplariVerir() {
        personelOlarakGir(true, a.getId());
        assertEquals(1, grupService.list().size());
        assertEquals(3, grupService.hedefListe().size());
    }

    @Test
    void kisitliPersonelDigerGrubunOgrencisineErisemez() {
        personelOlarakGir(true, a.getId());
        assertEquals(ogrA.getId(), ogrenciService.getById(ogrA.getId()).id());
        assertEquals(404, assertThrows(ApiException.class, () -> ogrenciService.getById(ogrB.getId())).getStatus().value());
        assertEquals(404, assertThrows(ApiException.class, () -> ogrenciService.delete(ogrB.getId())).getStatus().value());
        assertEquals(404, assertThrows(ApiException.class,
                () -> ogrenciService.list(b.getId(), null, false, false, 0, 50)).getStatus().value());
    }

    @Test
    void kisitliPersonelAramadaDigerGrupAdiylaEslesmez() {
        personelOlarakGir(true, a.getId());
        assertTrue(ogrenciService.list(null, "B Grubu", false, false, 0, 50).icerik().isEmpty());
    }

    @Test
    void kisitliPersonelOgrenciyiSadeceIzinliGrubaEkleyebilir() {
        personelOlarakGir(true, a.getId());
        assertEquals(404, assertThrows(ApiException.class,
                () -> ogrenciService.create(new OgrenciRequest(b.getId(), "Deniz", null, null, null, null, null, null, null))).getStatus().value());
        assertEquals(400, assertThrows(ApiException.class,
                () -> ogrenciService.create(new OgrenciRequest(null, "Deniz", null, null, null, null, null, null, null))).getStatus().value());
        assertEquals("Deniz", ogrenciService.create(new OgrenciRequest(a.getId(), "Deniz", null, null, null, null, null, null, null)).adSoyad());
    }

    @Test
    void kisitliPersonelGrupEkleyipSilemez() {
        personelOlarakGir(true, a.getId());
        assertEquals(403, assertThrows(ApiException.class,
                () -> grupService.delete(a.getId())).getStatus().value());
    }

    @Test
    void adminGrupYetkisiniVerirVeKaldirir() {
        KullaniciResponse r = kullaniciService.grupYetkisiniGuncelle(personel.getId(),
                new GrupYetkiGuncelleRequest(true, Set.of(a.getId(), b.getId())));
        assertTrue(r.grupKisitli());
        assertEquals(Set.of(a.getId(), b.getId()), r.grupIdleri());

        r = kullaniciService.grupYetkisiniGuncelle(personel.getId(), new GrupYetkiGuncelleRequest(false, null));
        assertEquals(false, r.grupKisitli());
        assertTrue(r.grupIdleri().isEmpty());
    }

    @Test
    void ogrenciIslemYetkileriOgrencilerMenusuOlmadanVerilemez() {
        var r = kullaniciService.yetkileriGuncelle(personel.getId(), new com.msc.halisaha.dto.YetkiGuncelleRequest(
                Set.of(com.msc.halisaha.entity.Modul.YOKLAMA, com.msc.halisaha.entity.Modul.OGRENCI_ODEME)));
        assertEquals(Set.of(com.msc.halisaha.entity.Modul.YOKLAMA), r.yetkiler());

        r = kullaniciService.yetkileriGuncelle(personel.getId(), new com.msc.halisaha.dto.YetkiGuncelleRequest(
                Set.of(com.msc.halisaha.entity.Modul.OGRENCI, com.msc.halisaha.entity.Modul.OGRENCI_ODEME)));
        assertEquals(Set.of(com.msc.halisaha.entity.Modul.OGRENCI, com.msc.halisaha.entity.Modul.OGRENCI_ODEME), r.yetkiler());
    }

    @Test
    void kisitlarkenEnAzBirGrupSecilmelidirVeGrupAyniIsletmedenOlmali() {
        assertEquals(400, assertThrows(ApiException.class, () -> kullaniciService.grupYetkisiniGuncelle(personel.getId(),
                new GrupYetkiGuncelleRequest(true, Set.of()))).getStatus().value());
        assertEquals(400, assertThrows(ApiException.class, () -> kullaniciService.grupYetkisiniGuncelle(personel.getId(),
                new GrupYetkiGuncelleRequest(true, Set.of(-5L)))).getStatus().value());
    }

    @Test
    void adminIcinGrupYetkisiTanimlanamazVeRolDegisinceSifirlanir() {
        assertEquals(400, assertThrows(ApiException.class, () -> kullaniciService.grupYetkisiniGuncelle(admin.getId(),
                new GrupYetkiGuncelleRequest(true, Set.of(a.getId())))).getStatus().value());

        kullaniciService.grupYetkisiniGuncelle(personel.getId(), new GrupYetkiGuncelleRequest(true, Set.of(a.getId())));
        kullaniciService.rolGuncelle(personel.getId(), new com.msc.halisaha.dto.RolGuncelleRequest(Rol.ADMIN));
        assertEquals(false, kullaniciService.getById(personel.getId()).grupKisitli());
    }
}
