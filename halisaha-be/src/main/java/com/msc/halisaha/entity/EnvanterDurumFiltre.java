package com.msc.halisaha.entity;

/** Öğrenci listesinde envanter durumuna göre filtre. */
public enum EnvanterDurumFiltre {
    /** Ödemesi alınmış ama teslim edilmemiş kalemi olanlar. */
    TESLIM_BEKLEYEN,
    /** En az bir envanter satışının tutarı tam ödenmemiş olanlar. */
    BORCLU,
    /** Hiç envanter satışı (envanter türü seçiliyse o türden kalemi) olmayanlar. */
    ALMAMIS,
    /** En az bir envanter satışı olan, hiçbirinde ödeme borcu ya da teslim bekleyen kalem kalmamış olanlar. */
    TAMAM
}
