package com.msc.halisaha.entity;

/** Admin tarafından PERSONEL kullanıcılarına ayrı ayrı verilebilen menü/ekran yetkileri. */
public enum Modul {
    OGRENCI,
    GRUP,
    SAHA,
    URUN,
    YOKLAMA,
    ENVANTER,
    HESAP,
    /** Öğrenci grup transfer taleplerini onaylama / reddetme ekranı. */
    TRANSFER,
    /** Abonelik (haftalık sabit rezervasyon) kayıt ve takip ekranı. */
    ABONELIK,
    /** Öğrenci detayındaki grup transferi kartı (Öğrenciler menüsü ile birlikte anlamlıdır). */
    OGRENCI_TRANSFER,
    /** Öğrenci detayındaki aylık ödemeler kartı. */
    OGRENCI_ODEME,
    /** Öğrenci detayındaki envanter satışları kartı. */
    OGRENCI_ENVANTER,
    /** Öğrenci detayındaki yoklama bilgileri kartı. */
    OGRENCI_YOKLAMA
}
