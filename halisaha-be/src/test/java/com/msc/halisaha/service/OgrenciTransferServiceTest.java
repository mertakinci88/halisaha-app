package com.msc.halisaha.service;

import com.msc.halisaha.TestOturum;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.dto.TransferKararRequest;
import com.msc.halisaha.dto.TransferResponse;
import com.msc.halisaha.dto.TransferTalepRequest;
import com.msc.halisaha.entity.Grup;
import com.msc.halisaha.entity.Isletme;
import com.msc.halisaha.entity.Kullanici;
import com.msc.halisaha.entity.Ogrenci;
import com.msc.halisaha.entity.OgrenciDurum;
import com.msc.halisaha.entity.Rol;
import com.msc.halisaha.entity.TransferDurum;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Her test geri alınan bir transaction'da çalışır; veritabanında kalıcı veri bırakmaz. */
@SpringBootTest
@Transactional
class OgrenciTransferServiceTest {

    @Autowired OgrenciTransferService transferService;
    @Autowired OgrenciRepository ogrenciRepository;
    @Autowired GrupRepository grupRepository;
    @Autowired IsletmeRepository isletmeRepository;
    @Autowired KullaniciRepository kullaniciRepository;

    private Kullanici a;
    private Kullanici b;
    private Grup minikler;
    private Grup yildizlar;
    private Grup baskaIsletmeGrubu;
    private Ogrenci ali;

    @BeforeEach
    void hazirla() {
        Isletme isletmeA = TestOturum.isletme(isletmeRepository, "A İşletmesi");
        Isletme isletmeB = TestOturum.isletme(isletmeRepository, "B İşletmesi");
        a = TestOturum.kullanici(kullaniciRepository, isletmeA, Rol.ADMIN);
        b = TestOturum.kullanici(kullaniciRepository, isletmeB, Rol.ADMIN);
        minikler = grupRepository.save(Grup.builder().isletme(isletmeA).ad("Minikler").build());
        yildizlar = grupRepository.save(Grup.builder().isletme(isletmeA).ad("Yıldızlar").build());
        baskaIsletmeGrubu = grupRepository.save(Grup.builder().isletme(isletmeB).ad("Başka").build());
        ali = ogrenciRepository.save(Ogrenci.builder().isletme(isletmeA).grup(minikler).adSoyad("Ali Demir").durum(OgrenciDurum.AKTIF).build());
        TestOturum.girisYap(a);
    }

    @AfterEach
    void temizle() {
        TestOturum.temizle();
    }

    private ApiException hata(Runnable is) {
        return assertThrows(ApiException.class, is::run);
    }

    @Test
    void talepAcilincaOgrenciGrubuDegismezOnaylaninceGecerVeGecmisteKalir() {
        TransferResponse talep = transferService.talepEt(ali.getId(), new TransferTalepRequest(yildizlar.getId(), "  Seviye uygun  "));
        assertEquals(TransferDurum.BEKLIYOR, talep.durum());
        assertEquals("Minikler", talep.kaynakGrupAd());
        assertEquals("Yıldızlar", talep.hedefGrupAd());
        assertEquals("Seviye uygun", talep.aciklama());
        assertEquals(minikler.getId(), ogrenciRepository.findById(ali.getId()).orElseThrow().getGrup().getId());
        assertEquals(1, transferService.bekleyenSayisi());

        TransferResponse onayli = transferService.onayla(talep.id());
        assertEquals(TransferDurum.ONAYLANDI, onayli.durum());
        assertNotNull(onayli.kararTarihi());
        assertEquals(yildizlar.getId(), ogrenciRepository.findById(ali.getId()).orElseThrow().getGrup().getId());

        List<TransferResponse> gecmis = transferService.ogrenciGecmisi(ali.getId());
        assertEquals(1, gecmis.size());
        assertEquals("Minikler", gecmis.get(0).kaynakGrupAd());
        assertEquals("Yıldızlar", gecmis.get(0).hedefGrupAd());
        assertEquals(0, transferService.bekleyenSayisi());
    }

    @Test
    void reddedilenVeIptalEdilenTalepGrubuDegistirmezKararVerilenTalepTekrarSonuclanmaz() {
        TransferResponse talep = transferService.talepEt(ali.getId(), new TransferTalepRequest(yildizlar.getId(), null));
        TransferResponse red = transferService.reddet(talep.id(), new TransferKararRequest("Kontenjan dolu"));
        assertEquals(TransferDurum.REDDEDILDI, red.durum());
        assertEquals("Kontenjan dolu", red.kararNotu());
        assertEquals(minikler.getId(), ogrenciRepository.findById(ali.getId()).orElseThrow().getGrup().getId());
        assertEquals(409, hata(() -> transferService.onayla(talep.id())).getStatus().value());

        TransferResponse yeni = transferService.talepEt(ali.getId(), new TransferTalepRequest(yildizlar.getId(), null));
        assertEquals(TransferDurum.IPTAL, transferService.iptalEt(yeni.id(), null).durum());
        assertEquals(2, transferService.ogrenciGecmisi(ali.getId()).size());
    }

    @Test
    void ayniGrubaVeyaBekleyenTalepVarkenYeniTalepVeBaskaIsletmeGrubuReddedilir() {
        assertEquals(400, hata(() -> transferService.talepEt(ali.getId(), new TransferTalepRequest(minikler.getId(), null))).getStatus().value());
        assertEquals(404, hata(() -> transferService.talepEt(ali.getId(), new TransferTalepRequest(baskaIsletmeGrubu.getId(), null))).getStatus().value());
        transferService.talepEt(ali.getId(), new TransferTalepRequest(yildizlar.getId(), null));
        assertEquals(409, hata(() -> transferService.talepEt(ali.getId(), new TransferTalepRequest(yildizlar.getId(), null))).getStatus().value());
    }

    @Test
    void talepSonrasiOgrenciGrubuDegistiyseOnayAnindakiGercekGrupKaydedilir() {
        TransferResponse talep = transferService.talepEt(ali.getId(), new TransferTalepRequest(yildizlar.getId(), null));
        Ogrenci o = ogrenciRepository.findById(ali.getId()).orElseThrow();
        o.setGrup(null);
        TransferResponse onayli = transferService.onayla(talep.id());
        assertNull(onayli.kaynakGrupAd());
        assertEquals("Yıldızlar", onayli.hedefGrupAd());
    }

    @Test
    void baskaIsletmeninTalebiGorulmezVeSonuclandirilamaz() {
        TransferResponse talep = transferService.talepEt(ali.getId(), new TransferTalepRequest(yildizlar.getId(), null));
        TestOturum.girisYap(b);
        assertEquals(404, hata(() -> transferService.onayla(talep.id())).getStatus().value());
        assertEquals(404, hata(() -> transferService.ogrenciGecmisi(ali.getId())).getStatus().value());
        assertEquals(0, transferService.listele(null).stream().filter(t -> t.id().equals(talep.id())).count());
    }

    @Test
    void bekleyenTalepinHedefiVeAciklamasiGuncellenirSonuclananGuncellenemez() {
        Grup ucuncu = grupRepository.save(Grup.builder().isletme(yildizlar.getIsletme()).ad("Şampiyonlar").build());
        TransferResponse talep = transferService.talepEt(ali.getId(), new TransferTalepRequest(yildizlar.getId(), "Yanlış"));

        TransferResponse guncel = transferService.guncelle(talep.id(), new TransferTalepRequest(ucuncu.getId(), "Düzeltildi"));
        assertEquals("Şampiyonlar", guncel.hedefGrupAd());
        assertEquals("Düzeltildi", guncel.aciklama());
        assertEquals(400, hata(() -> transferService.guncelle(talep.id(), new TransferTalepRequest(minikler.getId(), null))).getStatus().value());

        assertEquals("Şampiyonlar", transferService.onayla(talep.id()).hedefGrupAd());
        assertEquals(ucuncu.getId(), ogrenciRepository.findById(ali.getId()).orElseThrow().getGrup().getId());
        assertEquals(409, hata(() -> transferService.guncelle(talep.id(), new TransferTalepRequest(yildizlar.getId(), null))).getStatus().value());
    }
}
