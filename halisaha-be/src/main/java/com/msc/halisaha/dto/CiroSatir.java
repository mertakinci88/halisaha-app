package com.msc.halisaha.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Bir dönemin (gün/hafta/ay) cirosu. Alanlar rapora göre dolar:
 * ODEME esasında {@code nakit/kart/havaleEft} dolu, {@code sahaGeliri/urunGeliri} null;
 * SEANS esasında tersi. {@code adet}: ODEME'de tahsilat sayısı, SEANS'ta rezervasyon sayısı.
 */
public record CiroSatir(
        LocalDate donem,
        BigDecimal toplam,
        BigDecimal nakit,
        BigDecimal kart,
        BigDecimal havaleEft,
        BigDecimal sahaGeliri,
        BigDecimal urunGeliri,
        long adet
) {
}
