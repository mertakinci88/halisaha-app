package com.msc.halisaha.service;

import com.msc.halisaha.dto.EnvanterTurRequest;

import java.util.List;

/**
 * Testlerde kullanılan eski sabit envanter türleri. Türler artık işletme bazlı kayıt olduğundan, id'leri
 * {@link #kur(EnvanterTurService)} ile (oturumdaki işletme için) oluşturulduğunda dolar.
 */
enum TestTur {
    KALECI_FORMA("Kaleci Forma", true, "adet"),
    KISLIK_ESOFMAN("Kışlık Eşofman", true, "adet"),
    KRAMPON_CANTASI("Krampon Çantası", false, "adet"),
    TOZLUK("Tozluk", false, "çift"),
    YAGMURLUK("Yağmurluk", true, "adet"),
    YAZLIK_FORMA("Yazlık Forma", true, "adet");

    static final List<String> BEDENLER = List.of("128", "140", "152", "164", "176", "Small", "Medium");

    private final String ad;
    private final boolean bedenli;
    private final String birim;
    private Long id;

    TestTur(String ad, boolean bedenli, String birim) {
        this.ad = ad;
        this.bedenli = bedenli;
        this.birim = birim;
    }

    Long id() {
        return id;
    }

    /** Oturumdaki işletme için altı türü de oluşturur; id'ler son çağrıdaki işletmeye aittir. */
    static void kur(EnvanterTurService turService) {
        for (TestTur t : values()) {
            t.id = turService.create(new EnvanterTurRequest(t.ad, t.bedenli, t.birim, t.bedenli ? BEDENLER : null)).id();
        }
    }
}
