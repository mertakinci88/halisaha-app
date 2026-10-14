package com.msc.halisaha.dto;

import com.msc.halisaha.entity.OdemeYontemi;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** İade, kaynak alanlarından tam olarak biri dolu gönderilerek ilgili kayda bağlanır. */
public record IadeRequest(
        Long rezervasyonId,
        /** Öğrencinin ilgili aya ait ödeme kaydının id'si. */
        Long ogrenciOdemeId,
        Long envanterSatisId,
        Long abonelikId,
        @NotNull(message = "İade tutarı zorunludur") @DecimalMin(value = "0.01", message = "İade tutarı sıfırdan büyük olmalıdır") BigDecimal tutar,
        @NotNull(message = "İade şekli seçilmelidir") OdemeYontemi iadeSekli,
        /** Yalnızca Havale / EFT'de dikkate alınır ve o durumda zorunludur. */
        Long hesapId,
        /** Boşsa bugün; ileri tarih olamaz. */
        LocalDate iadeTarihi,
        @Size(max = 255, message = "Neden en fazla 255 karakter olabilir") String neden
) {
}
