package com.msc.halisaha.controller;

import com.msc.halisaha.dto.CiroRaporu;
import com.msc.halisaha.dto.EnvanterRaporu;
import com.msc.halisaha.dto.FinansRaporu;
import com.msc.halisaha.dto.KullaniciRaporlari;
import com.msc.halisaha.dto.OgrenciAylikRaporu;
import com.msc.halisaha.dto.OgrenciRaporu;
import com.msc.halisaha.dto.RezervasyonRaporu;
import com.msc.halisaha.dto.YoklamaRaporu;
import com.msc.halisaha.service.EnvanterRaporService;
import com.msc.halisaha.service.FinansRaporService;
import com.msc.halisaha.service.KullaniciRaporService;
import com.msc.halisaha.service.OgrenciRaporService;
import com.msc.halisaha.service.RezervasyonRaporService;
import com.msc.halisaha.service.YoklamaRaporService;
import com.msc.halisaha.service.RaporService;
import com.msc.halisaha.service.RaporService.Esas;
import com.msc.halisaha.service.RaporService.Grupla;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/raporlar")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RaporController {

    private final RaporService raporService;
    private final FinansRaporService finansRaporService;
    private final OgrenciRaporService ogrenciRaporService;
    private final YoklamaRaporService yoklamaRaporService;
    private final RezervasyonRaporService rezervasyonRaporService;
    private final EnvanterRaporService envanterRaporService;
    private final KullaniciRaporService kullaniciRaporService;

    /**
     * Rezervasyon cirosu. Varsayılanlar: son 30 gün, günlük, tahsilat tarihine (ODEME) göre.
     * {@code esas=SEANS} seans tarihine göre, iptal edilmeyen rezervasyonların saha/ürün gelirini verir.
     */
    @GetMapping("/ciro")
    public CiroRaporu ciro(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis,
            @RequestParam(required = false) Long sahaId,
            @RequestParam(defaultValue = "GUN") Grupla grupla,
            @RequestParam(defaultValue = "ODEME") Esas esas) {
        return raporService.ciro(baslangic, bitis, sahaId, grupla, esas);
    }

    /** Tahsil edilen / bekleyen ciro. Varsayılan dönem: bu ayın başından bugüne. */
    @GetMapping("/finans")
    public FinansRaporu finans(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis) {
        return finansRaporService.getir(baslangic, bitis);
    }

    /** Seçilen ayın öğrenci ödeme tahsilat özeti (varsayılan: bu ay). */
    @GetMapping("/ogrenci")
    public OgrenciRaporu ogrenci(@RequestParam(required = false) Integer yil, @RequestParam(required = false) Integer ay) {
        return ogrenciRaporService.getir(yil, ay);
    }

    /** Birden fazla aya yayılan dönemin (ör. bu yıl) ay ay öğrenci ödeme tahsilat özeti. */
    @GetMapping("/ogrenci/aylik")
    public OgrenciAylikRaporu ogrenciAylik(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis) {
        return ogrenciRaporService.aylik(baslangic, bitis);
    }

    @GetMapping("/yoklama")
    public YoklamaRaporu yoklama(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis) {
        return yoklamaRaporService.getir(baslangic, bitis);
    }

    @GetMapping("/rezervasyon")
    public RezervasyonRaporu rezervasyon(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis) {
        return rezervasyonRaporService.getir(baslangic, bitis);
    }

    @GetMapping("/envanter")
    public EnvanterRaporu envanter(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis) {
        return envanterRaporService.getir(baslangic, bitis);
    }

    /** Kullanıcı bazlı tahsilat (kasa): kim ne kadar para aldı, ne kadar iade etti. */
    @GetMapping("/kullanici/kasa")
    public KullaniciRaporlari.Kasa kullaniciKasa(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis) {
        return kullaniciRaporService.kasa(baslangic, bitis);
    }

    @GetMapping("/kullanici/islemler")
    public KullaniciRaporlari.Islemler kullaniciIslemler(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis) {
        return kullaniciRaporService.islemler(baslangic, bitis);
    }

    @GetMapping("/kullanici/iadeler")
    public KullaniciRaporlari.Iadeler kullaniciIadeler(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis) {
        return kullaniciRaporService.iadeler(baslangic, bitis);
    }

    @GetMapping("/kullanici/iptaller")
    public KullaniciRaporlari.Iptaller kullaniciIptaller(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis) {
        return kullaniciRaporService.iptaller(baslangic, bitis);
    }

    @GetMapping("/kullanici/yoklama")
    public KullaniciRaporlari.Yoklama kullaniciYoklama(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis) {
        return kullaniciRaporService.yoklama(baslangic, bitis);
    }

    /** Giriş / çıkış / başarısız giriş geçmişi; {@code kullaniciId} verilirse yalnızca o kullanıcı. */
    @GetMapping("/kullanici/giris")
    public KullaniciRaporlari.Giris kullaniciGiris(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis,
            @RequestParam(required = false) Long kullaniciId) {
        return kullaniciRaporService.giris(baslangic, bitis, kullaniciId);
    }
}
