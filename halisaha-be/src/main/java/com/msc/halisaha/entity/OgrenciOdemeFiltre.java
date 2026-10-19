package com.msc.halisaha.entity;

/** Öğrenci listesinde içinde bulunulan ayın ödeme durumuna göre filtre. */
public enum OgrenciOdemeFiltre {
    /** Ayı tamamlayan ("tamamı ödendi" ya da parasız kapatılmış) ödemesi olanlar. */
    ODENDI,
    /** Ayı tamamlayan ("tamamı ödendi") ödemesi olmayanlar: hiç ödemesi olmayan ve kısmi ödeme yapanlar. */
    BEKLEYEN,
    /** Bu ay hiç ödeme yapmamış ve ayı parasız da kapatılmamış olanlar. */
    HIC,
    /** Ödeme yapmış ama ayı tamamlamamış olanlar. */
    KISMI,
    /** Ayı tamamlamamış olanlar, ayın ödeme günü (15'i) geçmişse; öncesinde kimse gecikmiş sayılmaz. */
    GECIKMIS
}
