package com.msc.halisaha.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OdemeDurumuTest {

    private static BigDecimal tl(String s) {
        return new BigDecimal(s);
    }

    @Test
    void odemeYoksaOdenmedi() {
        assertEquals(OdemeDurumu.ODENMEDI, OdemeDurumu.hesapla(tl("0.00"), tl("1000.00")));
    }

    @Test
    void kismiOdemeKaporaDurumu() {
        assertEquals(OdemeDurumu.KISMI_ODENDI, OdemeDurumu.hesapla(tl("300.00"), tl("1000.00")));
    }

    @Test
    void tamOdemeOdendi() {
        assertEquals(OdemeDurumu.ODENDI, OdemeDurumu.hesapla(tl("1000.00"), tl("1000.00")));
    }

    @Test
    void urunEklenincePaketTekrarKismiOlur() {
        assertEquals(OdemeDurumu.KISMI_ODENDI, OdemeDurumu.hesapla(tl("1000.00"), tl("1060.00")));
    }
}
