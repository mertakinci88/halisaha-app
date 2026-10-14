package com.msc.halisaha.service;

import com.msc.halisaha.common.AppZaman;
import com.msc.halisaha.common.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** Rapor tarih aralığı: ilk ve son gün dahil. Varsayılan: bu ayın başından bugüne. */
public record RaporAraligi(LocalDate ilk, LocalDate son) {

    private static final long MAX_GUN = 366;

    /**
     * İşletme günü 12:00'da başlayıp ertesi gün 02:00'de biter; 00:00-02:00 arası kayıtlar
     * bir önceki güne yazılsın diye zaman 2 saat geri kaydırılarak gruplanır.
     */
    public static final String ISLETME_GUNU = "interval '2 hours'";

    /** İşletmenin günlük çalışma süresi (12:00 - 02:00). */
    public static final int GUNLUK_SAAT = 14;

    public static RaporAraligi of(LocalDate baslangic, LocalDate bitis) {
        LocalDate son = bitis != null ? bitis : AppZaman.bugun();
        LocalDate ilk = baslangic != null ? baslangic : son.withDayOfMonth(1);
        if (son.isBefore(ilk)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bitiş tarihi başlangıç tarihinden önce olamaz");
        }
        if (ChronoUnit.DAYS.between(ilk, son) > MAX_GUN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Rapor aralığı en fazla " + MAX_GUN + " gün olabilir");
        }
        return new RaporAraligi(ilk, son);
    }

    public LocalDateTime bas() {
        return ilk.atStartOfDay();
    }

    /** Dışlayıcı üst sınır (son günün ertesi 00:00). */
    public LocalDateTime bit() {
        return son.plusDays(1).atStartOfDay();
    }

    public long gunSayisi() {
        return ChronoUnit.DAYS.between(ilk, son) + 1;
    }

    /**
     * Karşılaştırma dönemi: aynı uzunlukta, hemen öncesindeki dönem. Yıl başından itibaren bir aralıkta
     * (1 Ocak - bugün gibi) geçen yılın aynı günleri alınır.
     */
    public RaporAraligi onceki() {
        if (ilk.getDayOfYear() == 1 && son.getYear() == ilk.getYear()) {
            return new RaporAraligi(ilk.minusYears(1), son.minusYears(1));
        }
        long gun = gunSayisi();
        return new RaporAraligi(ilk.minusDays(gun), ilk.minusDays(1));
    }
}
