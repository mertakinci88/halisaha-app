package com.msc.halisaha.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Finans özeti. Tahsil edilen: dönemde kasaya giren para (ödeme tarihine göre; envanter ve abonelik dahil), yapılan iadeler düşülmüş net tutardır.
 * Bekleyen: dönemde hizmeti verilen / satılan ama henüz ödenmemiş tutar.
 */
public record FinansRaporu(
        LocalDate baslangic,
        LocalDate bitis,
        /** GUN veya AY: trend satırlarının birimi. */
        String birim,
        Kaynak tahsilat,
        Kaynak oncekiTahsilat,
        LocalDate oncekiBaslangic,
        LocalDate oncekiBitis,
        /** Dönemde geri verilen tutar; tahsilat rakamları bu tutar düşülmüş (net) değerlerdir. */
        BigDecimal iade,
        Bekleyen bekleyen,
        /** Rezervasyonlara eklenen kafeterya ürünlerinin satış tutarı (ödeme durumundan bağımsız; tahsilat toplamına ayrıca eklenmez). */
        BigDecimal kafeteryaSatisi,
        Yontem yontem,
        List<Nokta> trend
) {
    public record Kaynak(BigDecimal rezervasyon, BigDecimal ogrenci, BigDecimal envanter, BigDecimal abonelik, BigDecimal toplam) {
    }

    public record Bekleyen(BigDecimal rezervasyon, BigDecimal envanter, BigDecimal abonelik, BigDecimal toplam) {
    }

    public record Yontem(BigDecimal nakit, BigDecimal kart, BigDecimal havaleEft) {
    }

    public record Nokta(LocalDate donem, BigDecimal rezervasyon, BigDecimal ogrenci, BigDecimal envanter, BigDecimal abonelik, BigDecimal toplam) {
    }
}
