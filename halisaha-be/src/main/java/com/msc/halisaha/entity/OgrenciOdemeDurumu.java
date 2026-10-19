package com.msc.halisaha.entity;

import java.time.LocalDate;

/** Öğrencinin bir aya ait ödeme durumu; saklanmaz, ödeme kayıtları ve bugünün tarihinden türetilir. */
public enum OgrenciOdemeDurumu {
    /** O ay için "tamamı ödendi" işaretli bir ödeme (ya da parasız kapatma) girilmiş. */
    ODENDI,
    /** Ödeme girilmiş ama ay tamamlanmamış; ödeme gününe (15'i) kadar süre var. */
    KISMI_ODENDI,
    /** Ödeme girilmiş ama ay tamamlanmamış ve ödeme günü geçmiş. */
    KISMI_GECIKMIS,
    /** Ödeme girilmemiş ama ayın ödeme gününe (15'i) kadar süre var. */
    ODEME_BEKLIYOR,
    /** Ödeme girilmemiş ve ödeme günü geçmiş. */
    ODENMEDI;

    /** Her ayın bu günü ödeme günüdür; bu günün sonuna kadar tamamlanmamış ay gecikmiş sayılmaz. */
    public static final int ODEME_GUNU = 15;

    /** @param tamam ayda tamamı ödendi işaretli kayıt var mı; @param kismi tamam değil ama ödenen tutar var mı */
    public static OgrenciOdemeDurumu hesapla(boolean tamam, boolean kismi, int yil, int ay, LocalDate bugun) {
        if (tamam) {
            return ODENDI;
        }
        boolean gecti = bugun.isAfter(LocalDate.of(yil, ay, ODEME_GUNU));
        if (kismi) {
            return gecti ? KISMI_GECIKMIS : KISMI_ODENDI;
        }
        return gecti ? ODENMEDI : ODEME_BEKLIYOR;
    }
}
