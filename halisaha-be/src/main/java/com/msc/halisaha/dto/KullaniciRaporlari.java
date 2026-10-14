package com.msc.halisaha.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Rapor ekranındaki "Kullanıcı" sekmesinin yanıtları (kasa, işlemler, iadeler, iptaller, yoklama, giriş geçmişi). */
public final class KullaniciRaporlari {

    private KullaniciRaporlari() {
    }

    /**
     * Kullanıcı bazlı tahsilat. Tahsilat: ödemeyi alan kullanıcıya yazılan, ödeme tarihine göre dönemdeki para; iade: o kullanıcının yaptığı iadeler.
     * kullaniciId null olan satır "Belirtilmemiş" (kullanıcı bilgisi tutulmadan önceki kayıtlar).
     */
    public record Kasa(LocalDate baslangic, LocalDate bitis, List<KasaSatiri> satirlar, KasaSatiri toplam) {
    }

    public record KasaSatiri(Long kullaniciId, String ad, String rol,
                             BigDecimal rezervasyon, BigDecimal ogrenci, BigDecimal envanter, BigDecimal abonelik,
                             BigDecimal tahsilat, BigDecimal nakit, BigDecimal kart, BigDecimal havale,
                             BigDecimal iade, BigDecimal iadeNakit,
                             BigDecimal net, BigDecimal netNakit,
                             /** Havale / EFT ile alınan paranın hangi hesaba (bankaya) yattığı; iadeler dahil değildir. */
                             List<HesapTutari> havaleHesaplari) {
    }

    public record HesapTutari(Long hesapId, String hesapAd, BigDecimal tutar) {
    }

    public record Islemler(LocalDate baslangic, LocalDate bitis, List<IslemSatiri> satirlar) {
    }

    public record IslemSatiri(Long kullaniciId, String ad, String rol,
                              long rezervasyon, long rezervasyonIptal, long ogrenciOdeme, long envanterSatis, long abonelik,
                              long yoklamaGunu, long iade, long transferTalebi,
                              long giris, LocalDateTime sonGiris, LocalDateTime sonCikis) {
    }

    public record Iadeler(LocalDate baslangic, LocalDate bitis, BigDecimal toplam, List<IadeKaynakToplami> kaynaklar,
                          List<IadeSatiri> satirlar, boolean kesildi) {
    }

    /** kaynak: REZERVASYON, OGRENCI, ENVANTER, ABONELIK */
    public record IadeKaynakToplami(String kaynak, long adet, BigDecimal toplam) {
    }

    public record IadeSatiri(Long id, LocalDate tarih, String kaynak, Long kaynakId, String ilgili, BigDecimal tutar,
                             String iadeSekli, String neden, String kullanici) {
    }

    public record Iptaller(LocalDate baslangic, LocalDate bitis, long toplam, List<IptalKisi> kisiler, List<IptalSatiri> satirlar,
                           boolean kesildi) {
    }

    public record IptalKisi(Long kullaniciId, String ad, long rezervasyon, long abonelik) {
    }

    /** tur: REZERVASYON ya da ABONELIK. seans: rezervasyonda seans zamanı, abonelikte başlangıç tarihi. iptalTarih eski kayıtlarda null olabilir. */
    public record IptalSatiri(String tur, Long id, String ad, LocalDateTime seans, String saha, LocalDateTime iptalTarih,
                              String iptalEden, String neden) {
    }

    public record Yoklama(LocalDate baslangic, LocalDate bitis, List<YoklamaGunu> gunler, List<YoklamaKullanici> kullanicilar, boolean kesildi) {
    }

    public record YoklamaGunu(LocalDate tarih, Long grupId, String grupAd, String kullanici, long ogrenci, long geldi, long gelmedi, long izinli) {
    }

    /** Yoklama alan kullanıcı: kaç gün, kaç farklı grup için yoklama aldığı ve son yoklaması. */
    public record YoklamaKullanici(Long kullaniciId, String ad, long yoklamaGunu, long grupSayisi, LocalDate sonYoklama) {
    }

    public record Giris(LocalDate baslangic, LocalDate bitis, List<GirisSatiri> satirlar, boolean kesildi) {
    }

    /** olay: GIRIS, CIKIS, OTURUM_SURESI_DOLDU, BASARISIZ_GIRIS */
    public record GirisSatiri(LocalDateTime zaman, Long kullaniciId, String kullaniciAdi, String ad, String olay, String ip, String tarayici) {
    }
}
