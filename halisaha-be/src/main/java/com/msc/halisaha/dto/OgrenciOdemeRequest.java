package com.msc.halisaha.dto;

import com.msc.halisaha.entity.OdemeYontemi;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Bir ayın tek bir ödemesi. Tutar boş ya da 0 ise açıklama (not) zorunludur; tamam = true ile 0 tutar, ayın parasız
 * kapatılması (indirim, burs vb.) demektir.
 */
public record OgrenciOdemeRequest(
        @PositiveOrZero(message = "Ödenen tutar negatif olamaz") BigDecimal odenenTutar,
        OdemeYontemi odemeSekli,
        /** Yalnızca Havale / EFT'de dikkate alınır. */
        Long hesapId,
        LocalDate odemeTarihi,
        @Size(max = 255, message = "Not en fazla 255 karakter olabilir") String not,
        /** true: bu ödemeyle ay tamamlanır (Ödendi); false: kısmi ödeme, ay açık kalır. */
        @NotNull(message = "Ödemenin tamamı mı kısmi mi olduğu seçilmelidir") Boolean tamam
) {
}
