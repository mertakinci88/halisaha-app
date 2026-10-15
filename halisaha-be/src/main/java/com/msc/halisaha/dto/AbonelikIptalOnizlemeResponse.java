package com.msc.halisaha.dto;

import java.math.BigDecimal;

/** İptal edilirse ne olacağının özeti (iptal ekranında gösterilir). */
public record AbonelikIptalOnizlemeResponse(
        /** İşletmede kalan depozito (alınan - iade). */
        BigDecimal kalanDepozito,
        BigDecimal haftalikUcret,
        /** Depozito ile tamamı karşılanıp oynanacak hafta sayısı. */
        int oynanacakHafta,
        /** İptal edilecek (oynanmayacak) hafta sayısı. */
        int iptalEdilecekHafta,
        /** Tam maça yetmeyen artan depozito. */
        BigDecimal artanTutar,
        /** Artan tutarla tamamlanabilecek bir sonraki hafta var mı (müşteri farkı ödeyerek oynar). */
        boolean kismiHaftaVar,
        /** Kısmi haftada müşterinin ödeyeceği fark ({@code kismiHaftaVar} ise dolu). */
        BigDecimal kismiHaftaOdenecek,
        /** Oynanacak hafta kalmadığı için artan depozito iade edilmek zorundadır. */
        boolean iadeZorunlu
) {
}
