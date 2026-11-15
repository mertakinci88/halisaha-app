package com.msc.halisaha.common;

import java.util.Locale;

/**
 * Arama metnini veritabanındaki tr_sade() fonksiyonuyla aynı kurala göre sadeleştirir:
 * büyük/küçük harf ve Türkçe karakter ayrımı (I/ı/İ/i, ş, ğ, ü, ö, ç) yoktur; ardışık boşluklar tek boşluğa iner.
 */
public final class AramaMetni {

    private static final String KAYNAK = "İIıŞşĞğÜüÖöÇçÂâÎîÛû";
    private static final String HEDEF = "iiissgguuooccaaiiuu";

    private AramaMetni() {
    }

    public static String sadelestir(String metin) {
        if (metin == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(metin.length());
        for (char c : metin.strip().toCharArray()) {
            int i = KAYNAK.indexOf(c);
            sb.append(i >= 0 ? HEDEF.charAt(i) : c);
        }
        return sb.toString().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
