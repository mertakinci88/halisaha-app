package com.msc.halisaha.entity;

import java.time.LocalDate;

/** Öğrencinin bir aya ait ödeme durumu; saklanmaz, ödeme kaydı ve bugünün tarihinden türetilir. */
public enum OgrenciOdemeDurumu {
    /** O ay için ödeme girilmiş. */
    ODENDI,
    /** Ödeme girilmemiş ama ayın ödeme gününe (15'i) kadar süre var. */
    ODEME_BEKLIYOR,
    /** Ödeme girilmemiş ve ödeme günü geçmiş. */
    ODENMEDI;

    /** Her ayın bu günü ödeme günüdür; bu günün sonuna kadar ödenmemiş ay "Ödeme bekliyor" sayılır. */
    public static final int ODEME_GUNU = 15;

    public static OgrenciOdemeDurumu hesapla(boolean odemeVar, int yil, int ay, LocalDate bugun) {
        if (odemeVar) {
            return ODENDI;
        }
        LocalDate sonGun = LocalDate.of(yil, ay, ODEME_GUNU);
        return bugun.isAfter(sonGun) ? ODENMEDI : ODEME_BEKLIYOR;
    }
}
