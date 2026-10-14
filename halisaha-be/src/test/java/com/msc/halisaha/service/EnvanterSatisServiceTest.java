package com.msc.halisaha.service;

import com.msc.halisaha.TestOturum;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.dto.EnvanterRequest;
import com.msc.halisaha.dto.EnvanterSatisRequest;
import com.msc.halisaha.dto.EnvanterSatisResponse;
import com.msc.halisaha.entity.EnvanterStokHareket;
import com.msc.halisaha.entity.EnvanterStokHareketTip;
import com.msc.halisaha.entity.EnvanterTur;
import com.msc.halisaha.entity.OdemeDurumu;
import com.msc.halisaha.entity.OdemeYontemi;
import com.msc.halisaha.repository.EnvanterRepository;
import com.msc.halisaha.repository.EnvanterStokHareketRepository;
import com.msc.halisaha.repository.IsletmeRepository;
import com.msc.halisaha.repository.KullaniciRepository;
import com.msc.halisaha.repository.OgrenciRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.msc.halisaha.dto.EnvanterSatisResponse.KalemResponse;
import static com.msc.halisaha.entity.EnvanterTur.KISLIK_ESOFMAN;
import static com.msc.halisaha.entity.EnvanterTur.KRAMPON_CANTASI;
import static com.msc.halisaha.entity.EnvanterTur.TOZLUK;
import static com.msc.halisaha.entity.EnvanterTur.YAGMURLUK;
import static com.msc.halisaha.entity.EnvanterTur.YAZLIK_FORMA;
import static com.msc.halisaha.service.EnvanterTestKurulum.set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Her test geri alınan bir transaction'da çalışır; veritabanında kalıcı veri bırakmaz. */
@SpringBootTest
@Transactional
class EnvanterSatisServiceTest {

    @Autowired EnvanterSatisService satisService;
    @Autowired EnvanterService envanterService;
    @Autowired OgrenciService ogrenciService;
    @Autowired EnvanterRepository envanterRepository;
    @Autowired EnvanterStokHareketRepository hareketRepository;
    @Autowired IsletmeRepository isletmeRepository;
    @Autowired KullaniciRepository kullaniciRepository;
    @Autowired OgrenciRepository ogrenciRepository;

    private EnvanterTestKurulum k;
    private Long ogrenci;

    @BeforeEach
    void hazirla() {
        k = new EnvanterTestKurulum(isletmeRepository, kullaniciRepository, ogrenciRepository, envanterService);
        ogrenci = k.ogrenciA.getId();
    }

    @AfterEach
    void temizle() {
        TestOturum.temizle();
    }

    private int stok(Long envanterId) {
        return envanterRepository.mevcutAdet(envanterId);
    }

    private ApiException hata(Runnable is) {
        return assertThrows(ApiException.class, is::run);
    }

    private KalemResponse kalem(EnvanterSatisResponse s, EnvanterTur tur) {
        return s.kalemler().stream().filter(x -> x.tur() == tur).findFirst().orElseThrow();
    }

    private long hareketSayisi(EnvanterStokHareketTip tip) {
        return hareketRepository.findAll().stream().filter(h -> h.getTip() == tip).count();
    }

    @Test
    void satisKaydedilirStokYalnizcaTeslimEdilenlerdenDuser() {
        long teslimOnce = hareketSayisi(EnvanterStokHareketTip.TESLIM);
        var satis = satisService.olustur(ogrenci, set("140", "164", List.of(TOZLUK, KRAMPON_CANTASI), List.of(TOZLUK, KRAMPON_CANTASI), null));

        assertEquals(0, new BigDecimal("2050").compareTo(satis.setTutari()));
        assertEquals(0, new BigDecimal("500").compareTo(satis.odenenTutar()));      // yalnızca Ödendi işaretlilerin toplamı (150 + 350)
        assertEquals(0, new BigDecimal("1550").compareTo(satis.kalanTutar()));
        assertEquals(OdemeDurumu.KISMI_ODENDI, satis.odemeDurumu());
        assertEquals(39, stok(k.tozluk.id()));
        assertEquals(21, stok(k.krampon.id()));
        assertEquals(5, stok(k.esofman140.id()));        // teslim edilmeyenin stoğu değişmez
        assertEquals(4, stok(k.yagmurluk164.id()));
        assertEquals(25, stok(k.forma128.id()));
        assertEquals(teslimOnce + 2, hareketSayisi(EnvanterStokHareketTip.TESLIM));
        assertTrue(kalem(satis, TOZLUK).teslimEdildi());
        assertFalse(kalem(satis, YAZLIK_FORMA).teslimEdildi());
        assertTrue(kalem(satis, KRAMPON_CANTASI).odendi());
        assertFalse(kalem(satis, YAGMURLUK).odendi());
    }

    @Test
    void tumEnvanterlerOdendiIseSetTutariOdenirVeDurumOdendiOlur() {
        var satis = satisService.olustur(ogrenci, set("140", "164", List.of(),
                List.of(KISLIK_ESOFMAN, KRAMPON_CANTASI, TOZLUK, YAGMURLUK, YAZLIK_FORMA), null));
        assertEquals(0, new BigDecimal("2050").compareTo(satis.odenenTutar()));
        assertEquals(OdemeDurumu.ODENDI, satis.odemeDurumu());
        assertEquals(0, BigDecimal.ZERO.compareTo(satis.kalanTutar()));
        assertEquals(40, stok(k.tozluk.id()));           // teslim yok: stok değişmedi
    }

    @Test
    void elleGirilenSatisFiyatiGecerliOlurEnvanterFiyatiDegismez() {
        var istek = new EnvanterSatisRequest(
                List.of(new EnvanterSatisRequest.Kalem(TOZLUK, null, true, false, new BigDecimal("100"))), null);
        var satis = satisService.olustur(ogrenci, istek);
        assertEquals(0, new BigDecimal("100").compareTo(kalem(satis, TOZLUK).birimFiyat()));
        assertEquals(0, new BigDecimal("100").compareTo(satis.setTutari()));
        assertEquals(0, new BigDecimal("100").compareTo(satis.odenenTutar()));

        var guncel = satisService.guncelle(ogrenci, satis.id(), new EnvanterSatisRequest(
                List.of(new EnvanterSatisRequest.Kalem(TOZLUK, null, true, false, new BigDecimal("120"))), null));
        assertEquals(0, new BigDecimal("120").compareTo(kalem(guncel, TOZLUK).birimFiyat()));
        assertEquals(0, new BigDecimal("120").compareTo(guncel.setTutari()));
    }

    @Test
    void odenenTutarElleDegisebilirAmaSetTutariniAsamazVeBosOlamaz() {
        var azi = satisService.olustur(ogrenci, set("140", "164", List.of(), List.of(TOZLUK), "450"));
        assertEquals(OdemeDurumu.KISMI_ODENDI, azi.odemeDurumu());
        assertEquals(400, hata(() -> satisService.olustur(ogrenci, set("140", "164", List.of(), List.of(TOZLUK), "2050.01"))).getStatus().value());
        assertEquals(400, hata(() -> satisService.olustur(ogrenci, set("140", "164", List.of(), List.of(), null))).getStatus().value());
        var tam = satisService.olustur(ogrenci, set("140", "164", List.of(), List.of(), "2050"));
        assertEquals(OdemeDurumu.ODENDI, tam.odemeDurumu());
    }

    @Test
    void stoksuzEnvanterSatilirAmaTeslimEdilemez() {
        // Eşofman Medium stoğu 0: satışa eklenebilir, teslim edilmeden bekler
        var satis = satisService.olustur(ogrenci, set("Medium", "164", List.of(), List.of(TOZLUK), null));
        assertFalse(kalem(satis, KISLIK_ESOFMAN).teslimEdildi());
        assertEquals(0, stok(k.esofmanMedium.id()));

        ApiException e = hata(() -> satisService.guncelle(ogrenci, satis.id(), set("Medium", "164", List.of(KISLIK_ESOFMAN), List.of(TOZLUK), null)));
        assertEquals(409, e.getStatus().value());
        assertTrue(e.getMessage().contains("Yetersiz stok"));
        assertEquals(0, stok(k.esofmanMedium.id()));

        // Stok girişi yapılınca teslim edilebilir
        envanterService.update(k.esofmanMedium.id(), new EnvanterRequest(KISLIK_ESOFMAN, "Medium", 5, new BigDecimal("600"), 5));
        var teslim = satisService.guncelle(ogrenci, satis.id(), set("Medium", "164", List.of(KISLIK_ESOFMAN), List.of(TOZLUK), null));
        assertTrue(kalem(teslim, KISLIK_ESOFMAN).teslimEdildi());
        assertEquals(4, stok(k.esofmanMedium.id()));
    }

    @Test
    void guncellemedeTeslimEdilinceDuserGeriAlininceStokGeriDoner() {
        var satis = satisService.olustur(ogrenci, set("140", "164", List.of(TOZLUK), List.of(TOZLUK), null));
        assertEquals(39, stok(k.tozluk.id()));
        long iadeOnce = hareketSayisi(EnvanterStokHareketTip.IADE);

        // Forma teslim edilir (stok düşer), Tozluk teslimi geri alınır (stok döner)
        var g = satisService.guncelle(ogrenci, satis.id(), set("140", "164", List.of(YAZLIK_FORMA), List.of(TOZLUK, YAZLIK_FORMA), null));
        assertEquals(24, stok(k.forma128.id()));
        assertEquals(40, stok(k.tozluk.id()));
        assertTrue(kalem(g, YAZLIK_FORMA).teslimEdildi());
        assertFalse(kalem(g, TOZLUK).teslimEdildi());
        assertEquals(iadeOnce + 1, hareketSayisi(EnvanterStokHareketTip.IADE));
        assertEquals(0, new BigDecimal("600").compareTo(g.odenenTutar()));        // 150 + 450
    }

    @Test
    void teslimEdilmeyenEnvanterinBedeniDegisebilirStokEtkilenmez() {
        var satis = satisService.olustur(ogrenci, set("140", "164", List.of(TOZLUK), List.of(TOZLUK), null));
        var g = satisService.guncelle(ogrenci, satis.id(), set("140", "Small", List.of(TOZLUK), List.of(TOZLUK), null));
        assertEquals("Small", kalem(g, YAGMURLUK).beden());
        assertEquals(4, stok(k.yagmurluk164.id()));            // teslim edilmediği için iki bedenin de stoğu aynı
        assertEquals(11, stok(k.yagmurlukSmall.id()));
        assertEquals(39, stok(k.tozluk.id()));
    }

    @Test
    void teslimEdilenEnvanterinBedeniDegistirilemezOnceTeslimGeriAlinmalidir() {
        var satis = satisService.olustur(ogrenci, set("140", "164", List.of(YAGMURLUK), List.of(), "100"));
        assertEquals(3, stok(k.yagmurluk164.id()));

        ApiException e = hata(() -> satisService.guncelle(ogrenci, satis.id(), set("140", "Small", List.of(YAGMURLUK), List.of(), "100")));
        assertEquals(400, e.getStatus().value());
        assertTrue(e.getMessage().contains("bedeni değiştirilemez"));
        // Teslimi geri alırken beden değişimi de aynı istekte reddedilir
        assertEquals(400, hata(() -> satisService.guncelle(ogrenci, satis.id(), set("140", "Small", List.of(), List.of(), "100"))).getStatus().value());
        assertEquals(3, stok(k.yagmurluk164.id()));
        assertEquals(11, stok(k.yagmurlukSmall.id()));

        // Önce teslimi geri al (aynı beden), sonra bedeni değiştir
        satisService.guncelle(ogrenci, satis.id(), set("140", "164", List.of(), List.of(), "100"));
        assertEquals(4, stok(k.yagmurluk164.id()));
        var g = satisService.guncelle(ogrenci, satis.id(), set("140", "Small", List.of(), List.of(), "100"));
        assertEquals("Small", kalem(g, YAGMURLUK).beden());
    }

    @Test
    void satisFiyatiSatisAnindakiKopyadirSonradanDegisenFiyatGecmisiEtkilemez() {
        var satis = satisService.olustur(ogrenci, set("140", "164", List.of(), List.of(TOZLUK), null));
        envanterService.update(k.forma128.id(), new EnvanterRequest(YAZLIK_FORMA, "128", 25, new BigDecimal("999"), 5));

        var ayni = satisService.guncelle(ogrenci, satis.id(), set("140", "164", List.of(), List.of(TOZLUK), null));
        assertEquals(0, new BigDecimal("450").compareTo(kalem(ayni, YAZLIK_FORMA).birimFiyat()));
        assertEquals(0, new BigDecimal("2050").compareTo(ayni.setTutari()));

        var yeni = satisService.olustur(ogrenci, set("140", "164", List.of(), List.of(TOZLUK), null));
        assertEquals(0, new BigDecimal("999").compareTo(kalem(yeni, YAZLIK_FORMA).birimFiyat()));
        assertEquals(0, new BigDecimal("2599").compareTo(yeni.setTutari()));
        assertEquals(2, satisService.listele(ogrenci).size());     // aynı öğrenciye birden fazla set satılabilir
    }

    @Test
    void satisSilinirseTeslimEdilenlerinStoguGeriDonerSilinenSatisListedenCikar() {
        var satis = satisService.olustur(ogrenci, set("140", "164", List.of(TOZLUK, KISLIK_ESOFMAN), List.of(), "100"));
        assertEquals(39, stok(k.tozluk.id()));
        assertEquals(4, stok(k.esofman140.id()));

        satisService.sil(ogrenci, satis.id());
        assertEquals(40, stok(k.tozluk.id()));
        assertEquals(5, stok(k.esofman140.id()));
        assertEquals(4, stok(k.yagmurluk164.id()));              // hiç teslim edilmemişti
        assertTrue(satisService.listele(ogrenci).isEmpty());
        assertEquals(404, hata(() -> satisService.sil(ogrenci, satis.id())).getStatus().value());
    }

    @Test
    void hataliIstekHicbirStokuDegistirmez() {
        // Boş liste, tekrarlı kalem, tanımsız beden, kayıtsız envanter
        assertEquals(400, hata(() -> satisService.olustur(ogrenci, new EnvanterSatisRequest(List.of(), new BigDecimal("100")))).getStatus().value());

        List<EnvanterSatisRequest.Kalem> tekrar = new ArrayList<>(set("140", "164", List.of(), List.of(), "100").kalemler());
        tekrar.add(tekrar.get(0));
        assertEquals(400, hata(() -> satisService.olustur(ogrenci, new EnvanterSatisRequest(tekrar, new BigDecimal("100")))).getStatus().value());

        assertEquals(400, hata(() -> satisService.olustur(ogrenci, set("Large", "164", List.of(), List.of(), "100"))).getStatus().value());
        assertEquals(404, hata(() -> satisService.olustur(ogrenci, set("152", "164", List.of(), List.of(), "100"))).getStatus().value());
        assertEquals(40, stok(k.tozluk.id()));
    }

    @Test
    void setYalnizcaSecilenKayitliEnvanterlerdenOlusabilir() {
        // Yalnızca Tozluk + Krampon satılır; diğer türler sette yer almaz
        var satis = satisService.olustur(ogrenci, new EnvanterSatisRequest(List.of(
                new EnvanterSatisRequest.Kalem(TOZLUK, null, true, true),
                new EnvanterSatisRequest.Kalem(KRAMPON_CANTASI, null, false, false)), null));
        assertEquals(2, satis.kalemler().size());
        assertEquals(0, new BigDecimal("500").compareTo(satis.setTutari()));       // 150 + 350
        assertEquals(0, new BigDecimal("150").compareTo(satis.odenenTutar()));      // yalnızca Ödendi işaretli
        assertEquals(39, stok(k.tozluk.id()));
        assertEquals(22, stok(k.krampon.id()));

        var g = satisService.guncelle(ogrenci, satis.id(), new EnvanterSatisRequest(List.of(
                new EnvanterSatisRequest.Kalem(TOZLUK, null, true, true),
                new EnvanterSatisRequest.Kalem(KRAMPON_CANTASI, null, true, true)), null));
        assertEquals(OdemeDurumu.ODENDI, g.odemeDurumu());
        assertEquals(21, stok(k.krampon.id()));

        // Güncellemede envanter çıkarılabilir: teslim edilmiş kalemin stoğu geri döner
        var cikarilmis = satisService.guncelle(ogrenci, satis.id(), new EnvanterSatisRequest(List.of(
                new EnvanterSatisRequest.Kalem(KRAMPON_CANTASI, null, true, true)), null));
        assertEquals(1, cikarilmis.kalemler().size());
        assertEquals(0, new BigDecimal("350").compareTo(cikarilmis.setTutari()));
        assertEquals(40, stok(k.tozluk.id()));
        assertEquals(21, stok(k.krampon.id()));
        assertEquals(400, hata(() -> satisService.guncelle(ogrenci, satis.id(), new EnvanterSatisRequest(List.of(), null))).getStatus().value());

        // ... ve sonradan eklenebilir (eklenen kalem teslimsizse stok düşmez)
        var eklenmis = satisService.guncelle(ogrenci, satis.id(), set("140", "164", List.of(KRAMPON_CANTASI), List.of(KRAMPON_CANTASI), null));
        assertEquals(5, eklenmis.kalemler().size());
        assertEquals(0, new BigDecimal("2050").compareTo(eklenmis.setTutari()));   // 150 + 350 + 600 + 500 + 450
        assertEquals(40, stok(k.tozluk.id()));
        assertEquals(21, stok(k.krampon.id()));

        // Teslimle eklenen kalemin stoğu düşer; satış silinince hepsi geri döner
        satisService.guncelle(ogrenci, satis.id(), set("140", "164", List.of(KRAMPON_CANTASI, TOZLUK), List.of(KRAMPON_CANTASI), null));
        assertEquals(39, stok(k.tozluk.id()));
        satisService.sil(ogrenci, satis.id());
        assertEquals(40, stok(k.tozluk.id()));
        assertEquals(22, stok(k.krampon.id()));
    }

    @Test
    void teslimBekleyenlerYalnizcaOdenmisTeslimEdilmemisKalemleriGosterir() {
        assertEquals(0, satisService.teslimBekleyenler().kalemSayisi());
        // Tozluk ödendi + teslim, Krampon ödendi (teslim yok), Forma ödenmedi
        satisService.olustur(ogrenci, new EnvanterSatisRequest(List.of(
                new EnvanterSatisRequest.Kalem(TOZLUK, null, true, true),
                new EnvanterSatisRequest.Kalem(KRAMPON_CANTASI, null, true, false),
                new EnvanterSatisRequest.Kalem(YAZLIK_FORMA, "128", false, false)), null));
        var bekleyen = satisService.teslimBekleyenler();
        assertEquals(1, bekleyen.kalemSayisi());
        assertEquals(1, bekleyen.ogrenciler().size());
        assertEquals("Ahmet Test", bekleyen.ogrenciler().get(0).adSoyad());
        assertEquals("Krampon Çantası", bekleyen.ogrenciler().get(0).kalemler().get(0).turAd());
        assertFalse(bekleyen.ogrenciler().get(0).kalemler().get(0).stokYok());

        // Başka işletme göremez
        TestOturum.girisYap(k.b);
        assertEquals(0, satisService.teslimBekleyenler().kalemSayisi());
    }

    @Test
    void envanterSatisiOlanOgrenciSilinemezSatisSilinincesilinebilir() {
        var satis = satisService.olustur(ogrenci, set("140", "164", List.of(TOZLUK), List.of(TOZLUK), null));
        ApiException e = hata(() -> ogrenciService.delete(ogrenci));
        assertEquals(409, e.getStatus().value());
        satisService.sil(ogrenci, satis.id());
        ogrenciService.delete(ogrenci);
    }

    @Test
    void baskaIsletmeSatisGoremezDegistiremezOgrenciyeErisemez() {
        var satis = satisService.olustur(ogrenci, set("140", "164", List.of(TOZLUK), List.of(TOZLUK), null));

        TestOturum.girisYap(k.b);
        assertEquals(404, hata(() -> satisService.listele(ogrenci)).getStatus().value());
        assertEquals(404, hata(() -> satisService.olustur(ogrenci, set("140", "164", List.of(), List.of(), "100"))).getStatus().value());
        assertEquals(404, hata(() -> satisService.guncelle(ogrenci, satis.id(), set("140", "164", List.of(), List.of(), "100"))).getStatus().value());
        assertEquals(404, hata(() -> satisService.sil(ogrenci, satis.id())).getStatus().value());

        TestOturum.girisYap(k.a);
        assertEquals(1, satisService.listele(ogrenci).size());
        assertEquals(39, stok(k.tozluk.id()));
    }

    @Test
    void stokHareketleriTeslimVeIadeyiKaydeder() {
        var satis = satisService.olustur(ogrenci, set("140", "164", List.of(TOZLUK), List.of(TOZLUK), null));
        satisService.guncelle(ogrenci, satis.id(), set("140", "164", List.of(), List.of(TOZLUK), null));
        List<EnvanterStokHareket> hareketler = hareketRepository.findAll().stream()
                .filter(h -> h.getEnvanterId().equals(k.tozluk.id())).toList();
        // ilk stok girişi (DUZELTME), teslim (-1, kalan 39), iade (+1, kalan 40)
        assertEquals(List.of(EnvanterStokHareketTip.DUZELTME, EnvanterStokHareketTip.TESLIM, EnvanterStokHareketTip.IADE),
                hareketler.stream().map(EnvanterStokHareket::getTip).toList());
        assertEquals(List.of(40, 39, 40), hareketler.stream().map(EnvanterStokHareket::getKalanAdet).toList());
    }

    @Test
    void ogrenciListesiEksikEnvanterFiltresiYalnizcaOdenipTeslimEdilmeyenleriGetirir() {
        var isletme = k.ogrenciA.getIsletme();
        Long teslimEdilmis = ogrenciRepository.save(com.msc.halisaha.entity.Ogrenci.builder().isletme(isletme).adSoyad("Teslim Edilmiş")
                .durum(com.msc.halisaha.entity.OgrenciDurum.AKTIF).build()).getId();
        ogrenciRepository.save(com.msc.halisaha.entity.Ogrenci.builder().isletme(isletme).adSoyad("Envanteri Yok")
                .durum(com.msc.halisaha.entity.OgrenciDurum.AKTIF).build());

        // Ahmet Test: tozluk ödenmiş ama teslim edilmemiş -> eksik
        satisService.olustur(ogrenci, set("140", "164", List.of(), List.of(TOZLUK), null));
        // Teslim Edilmiş: tozluk hem ödenmiş hem teslim edilmiş -> eksik değil
        satisService.olustur(teslimEdilmis, set("140", "164", List.of(TOZLUK), List.of(TOZLUK), null));

        var eksikler = ogrenciService.list(null, null, false, true, 0, 50).icerik();
        assertEquals(List.of("Ahmet Test"), eksikler.stream().map(com.msc.halisaha.dto.OgrenciResponse::adSoyad).toList());
        assertEquals(3, ogrenciService.list(null, null, false, false, 0, 50).toplamKayit());
    }

    private EnvanterSatisRequest toplamOdenenle(EnvanterSatisRequest oncekiIstek, String toplam, OdemeYontemi yontem, Long hesapId) {
        return new EnvanterSatisRequest(oncekiIstek.kalemler(), new BigDecimal(toplam), null, yontem, hesapId);
    }

    @Test
    void sonradanAlinanOdemeKendiYontemiyleAyriSatirOlurAzalincaSonOdemelerdenDuser() {
        var istek = set("140", "164", List.of(), List.of(TOZLUK), "450");
        var satis = satisService.olustur(ogrenci, istek);
        assertEquals(1, satis.odemeler().size());
        assertEquals(OdemeYontemi.NAKIT, satis.odemeler().get(0).odemeSekli());

        // Toplam artınca yalnızca fark yeni ödeme olur ve istekteki yöntemle yazılır
        var g = satisService.guncelle(ogrenci, satis.id(), toplamOdenenle(istek, "800", OdemeYontemi.KART, null));
        assertEquals(2, g.odemeler().size());
        assertEquals(0, new BigDecimal("450").compareTo(g.odemeler().get(0).tutar()));
        assertEquals(OdemeYontemi.NAKIT, g.odemeler().get(0).odemeSekli());
        assertEquals(0, new BigDecimal("350").compareTo(g.odemeler().get(1).tutar()));
        assertEquals(OdemeYontemi.KART, g.odemeler().get(1).odemeSekli());
        assertEquals(0, new BigDecimal("800").compareTo(g.odenenTutar()));

        // Yeni ödeme olmuyorsa yöntem gerekmez; artış varsa yöntem ve (Havale / EFT'de) hesap zorunludur
        assertEquals(2, satisService.guncelle(ogrenci, satis.id(), toplamOdenenle(istek, "800", null, null)).odemeler().size());
        assertEquals(400, hata(() -> satisService.guncelle(ogrenci, satis.id(), toplamOdenenle(istek, "900", null, null))).getStatus().value());
        assertEquals(400, hata(() -> satisService.guncelle(ogrenci, satis.id(), toplamOdenenle(istek, "900", OdemeYontemi.HAVALE_EFT, null))).getStatus().value());

        // Toplam azalınca en son ödemeden başlanarak düşülür
        var az = satisService.guncelle(ogrenci, satis.id(), toplamOdenenle(istek, "300", null, null));
        assertEquals(1, az.odemeler().size());
        assertEquals(0, new BigDecimal("300").compareTo(az.odemeler().get(0).tutar()));
    }

    @Test
    void odemeSilinirToplamAzalirAmaSonOdemeSilinemez() {
        var istek = set("140", "164", List.of(), List.of(TOZLUK), "450");
        var satis = satisService.olustur(ogrenci, istek);
        var iki = satisService.guncelle(ogrenci, satis.id(), toplamOdenenle(istek, "800", OdemeYontemi.KART, null));

        var tek = satisService.odemeSil(ogrenci, satis.id(), iki.odemeler().get(1).id());
        assertEquals(1, tek.odemeler().size());
        assertEquals(0, new BigDecimal("450").compareTo(tek.odenenTutar()));
        assertEquals(0, new BigDecimal("1600").compareTo(tek.kalanTutar()));
        assertEquals(400, hata(() -> satisService.odemeSil(ogrenci, satis.id(), tek.odemeler().get(0).id())).getStatus().value());
        assertEquals(404, hata(() -> satisService.odemeSil(ogrenci, satis.id(), 999999L)).getStatus().value());
    }
}
