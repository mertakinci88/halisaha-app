package com.msc.halisaha.dto;

import com.msc.halisaha.entity.EnvanterTur;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

/** Satış seti: işletmede kaydı olan envanter türlerinden, her türden en fazla bir kalem (en az bir kalem). */
public record EnvanterSatisRequest(
        @NotNull(message = "Kalem listesi zorunludur") List<@Valid Kalem> kalemler,
        /** Boşsa, Ödendi işaretli kalemlerin fiyat toplamı alınır. Set tutarını aşamaz. */
        @PositiveOrZero(message = "Ödenen tutar negatif olamaz") BigDecimal odenenTutar,
        /** Satışın başlatıldığı set; boşsa özel satıştır. Yalnızca satış oluşturulurken kullanılır, sonradan değişmez. */
        Long setId
) {

    public EnvanterSatisRequest(List<Kalem> kalemler, BigDecimal odenenTutar) {
        this(kalemler, odenenTutar, null);
    }

    public record Kalem(
            @NotNull(message = "Envanter türü zorunludur") EnvanterTur tur,
            /** Bedenli türlerde zorunlu. */
            String beden,
            boolean odendi,
            boolean teslim
    ) {
    }
}
