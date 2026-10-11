package com.msc.halisaha.entity;

public enum EnvanterStokHareketTip {
    /** Envanter öğrenciye teslim edildi, stoktan düştü. */
    TESLIM,
    /** Teslim geri alındı ya da satış silindi, stoğa döndü. */
    IADE,
    /** Envanter ekranından elle yapılan stok değişikliği (ilk giriş dahil). */
    DUZELTME
}
