package com.msc.halisaha.common;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.function.Function;

/**
 * Türkçe alfabe sırası (A, B, C, Ç, ... I, İ, ... O, Ö, ... S, Ş, ... U, Ü ...).
 * Veritabanının sıralaması sunucunun locale'ine bağlı olduğu için liste sıralamaları burada yapılır.
 */
public final class TurkceSirala {

    private static final Collator TR = Collator.getInstance(Locale.forLanguageTag("tr-TR"));

    private TurkceSirala() {
    }

    /** Verilen metne göre Türkçe alfabetik sıralar; boş/null değerler sona gider. */
    public static <T> Comparator<T> alfabetik(Function<T, String> metin) {
        return Comparator.comparing(metin, Comparator.nullsLast((a, b) -> TR.compare(a, b)));
    }
}
