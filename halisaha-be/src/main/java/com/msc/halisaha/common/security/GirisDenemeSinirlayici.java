package com.msc.halisaha.common.security;

import com.msc.halisaha.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Parola deneme saldırılarını yavaşlatır: aynı adresten art arda çok başarısız giriş olursa o adres geçici olarak
 * engellenir. Sayaçlar bellektedir (tek sunucu); yeniden başlatmada sıfırlanır.
 * Kullanıcı adı tek başına kilitlenmez; aksi halde bir saldırgan gerçek kullanıcının girişini engelleyebilirdi.
 */
@Component
public class GirisDenemeSinirlayici {

    static final int ADRES_KULLANICI_LIMIT = 5;
    static final int ADRES_LIMIT = 15;
    static final Duration PENCERE = Duration.ofMinutes(15);

    private record Sayac(int adet, Instant ilk) {}

    private final ConcurrentHashMap<String, Sayac> sayaclar = new ConcurrentHashMap<>();

    /** Adres engelliyse 429 fırlatır. */
    public void kontrolEt(IstemciBilgisi bilgi, String kullaniciAdi) {
        temizle();
        if (dolu(adresAnahtari(bilgi), ADRES_LIMIT) || dolu(adresKullaniciAnahtari(bilgi, kullaniciAdi), ADRES_KULLANICI_LIMIT)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Çok fazla başarısız giriş denemesi yapıldı. Lütfen " + PENCERE.toMinutes() + " dakika sonra tekrar deneyin");
        }
    }

    public void basarisiz(IstemciBilgisi bilgi, String kullaniciAdi) {
        Instant simdi = Instant.now();
        for (String anahtar : new String[]{adresAnahtari(bilgi), adresKullaniciAnahtari(bilgi, kullaniciAdi)}) {
            sayaclar.merge(anahtar, new Sayac(1, simdi), (eski, yeni) -> new Sayac(eski.adet() + 1, eski.ilk()));
        }
    }

    public void basarili(IstemciBilgisi bilgi, String kullaniciAdi) {
        sayaclar.remove(adresKullaniciAnahtari(bilgi, kullaniciAdi));
    }

    private boolean dolu(String anahtar, int limit) {
        Sayac sayac = sayaclar.get(anahtar);
        return sayac != null && sayac.adet() >= limit;
    }

    private void temizle() {
        Instant sinir = Instant.now().minus(PENCERE);
        sayaclar.values().removeIf(s -> s.ilk().isBefore(sinir));
    }

    private static String adresAnahtari(IstemciBilgisi bilgi) {
        return "a:" + bilgi.ip();
    }

    private static String adresKullaniciAnahtari(IstemciBilgisi bilgi, String kullaniciAdi) {
        return "k:" + bilgi.ip() + "|" + (kullaniciAdi == null ? "" : kullaniciAdi.toLowerCase());
    }
}
