package com.msc.halisaha.entity;

/** Satılan envanter türleri. Sıra, listelerdeki (Türkçe alfabetik) sıradır. */
public enum EnvanterTur {
    KISLIK_ESOFMAN("Kışlık Eşofman", true),
    KRAMPON_CANTASI("Krampon Çantası", false),
    TOZLUK("Tozluk", false),
    YAGMURLUK("Yağmurluk", true),
    YAZLIK_FORMA("Yazlık Forma", true);

    private final String ad;
    private final boolean bedenli;

    EnvanterTur(String ad, boolean bedenli) {
        this.ad = ad;
        this.bedenli = bedenli;
    }

    public String getAd() {
        return ad;
    }

    /** Bedenli türlerde beden seçilir; diğerlerinde beden STANDART tutulur. */
    public boolean isBedenli() {
        return bedenli;
    }
}
