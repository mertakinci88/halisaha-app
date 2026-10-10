package com.msc.halisaha.entity;

public enum StokHareketTip {
    /** Ürün rezervasyona eklendi, stoktan düştü. */
    SATIS,
    /** Ürün rezervasyondan çıkarıldı / rezervasyon iptal edildi veya silindi, stoğa döndü. */
    IADE,
    /** Ürün ekranından elle yapılan stok değişikliği. */
    DUZELTME
}
