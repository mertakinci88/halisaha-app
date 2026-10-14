package com.msc.halisaha.dto;

import com.msc.halisaha.entity.EnvanterTur;
import com.msc.halisaha.entity.OdemeYontemi;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

/** Satış seti: işletmede kaydı olan envanter türlerinden, her türden en fazla bir kalem (en az bir kalem). */
public record EnvanterSatisRequest(
        @NotNull(message = "Kalem listesi zorunludur") List<@Valid Kalem> kalemler,
        /**
         * Satış kaydedildikten sonra toplam ödenmiş tutar. Boşsa Ödendi işaretli kalemlerin fiyat toplamı alınır; set tutarını aşamaz.
         * Mevcut ödemelerin toplamından büyükse aradaki fark yeni bir ödeme olarak yazılır (yöntemi aşağıdaki alanlardır);
         * küçükse en son ödemelerden düşülür.
         */
        @PositiveOrZero(message = "Ödenen tutar negatif olamaz") BigDecimal odenenTutar,
        /** Satışın başlatıldığı set; boşsa özel satıştır. Yalnızca satış oluşturulurken kullanılır, sonradan değişmez. */
        Long setId,
        /** Yeni eklenen ödemenin yöntemi; yeni ödeme oluşuyorsa zorunludur. */
        OdemeYontemi odemeSekli,
        /** Yeni ödeme Havale / EFT ise paranın yattığı hesap (zorunlu). */
        Long hesapId
) {

    // Yalnızca testler için kısayollar: ödeme yöntemi Nakit kabul edilir
    public EnvanterSatisRequest(List<Kalem> kalemler, BigDecimal odenenTutar) {
        this(kalemler, odenenTutar, null, OdemeYontemi.NAKIT, null);
    }

    public EnvanterSatisRequest(List<Kalem> kalemler, BigDecimal odenenTutar, Long setId) {
        this(kalemler, odenenTutar, setId, OdemeYontemi.NAKIT, null);
    }

    public record Kalem(
            @NotNull(message = "Envanter türü zorunludur") EnvanterTur tur,
            /** Bedenli türlerde zorunlu. */
            String beden,
            boolean odendi,
            boolean teslim,
            /** Boşsa envanterin satış fiyatı (güncellemede kalemin mevcut fiyatı) kullanılır; doluysa bu satışa özel fiyat geçerli olur. */
            @PositiveOrZero(message = "Satış fiyatı negatif olamaz") BigDecimal birimFiyat
    ) {

        public Kalem(EnvanterTur tur, String beden, boolean odendi, boolean teslim) {
            this(tur, beden, odendi, teslim, null);
        }
    }
}
