package com.msc.halisaha.entity;

import java.util.List;

/** Geçerli beden değerleri (liste sırası, ekrandaki seçim sırasıdır). */
public final class EnvanterBeden {

    public static final String STANDART = "STANDART";
    public static final List<String> BEDENLER = List.of("128", "140", "152", "164", "176", "Small", "Medium");

    private EnvanterBeden() {
    }

    public static boolean gecerli(String beden) {
        return beden != null && BEDENLER.contains(beden);
    }

    /** Sıralama anahtarı: STANDART başa, sonra tanımlı beden sırası. */
    public static int sira(String beden) {
        return STANDART.equals(beden) ? -1 : BEDENLER.indexOf(beden);
    }
}
