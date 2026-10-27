package com.msc.halisaha.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Bir yılın 12 ayı ve yıl toplamı. */
public record OgrenciOdemeYilResponse(
        int yil,
        /** Sunucunun bugünü (durum hesabı bu tarihe göre yapılır). */
        LocalDate bugun,
        List<OgrenciOdemeAyResponse> aylar,
        BigDecimal toplamOdenen,
        /** Kayıtlı yıllar (yıl seçicisi için); seçili yıl ve bu yıl dahil. */
        List<Integer> yillar,
        /** Kapalıysa öğrenciden ödeme alınmaz (yeni ödeme eklenemez, aylar için borç hesaplanmaz). */
        boolean odemeAlinir,
        /** Öğrencinin herhangi bir yılda ödeme kaydı var mı. */
        boolean odemeKaydiVar
) {
}
