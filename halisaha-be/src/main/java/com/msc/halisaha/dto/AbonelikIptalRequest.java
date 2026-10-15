package com.msc.halisaha.dto;

import com.msc.halisaha.entity.OdemeYontemi;
import jakarta.validation.constraints.Size;

/**
 * Abonelik iptali. Kalan depozito ile kalan haftalar oynanır; depozito bir maça yetmeyen küçük bir tutar artarsa
 * ya {@code artanIade} ile müşteriye iade edilir ya da son maça sayılır (müşteri farkı öder).
 * Oynanacak haftası kalmamışsa artan depozito her durumda iade edilir. İade tutarın kendisi sunucuda hesaplanır.
 */
public record AbonelikIptalRequest(
        @Size(max = 255, message = "Neden en fazla 255 karakter olabilir") String neden,
        /** Artan tutar son maça sayılmasın, iade edilsin. */
        Boolean artanIade,
        /** İade gerekiyorsa zorunludur. */
        OdemeYontemi iadeSekli,
        /** Yalnızca Havale / EFT iadesinde dikkate alınır ve o durumda zorunludur. */
        Long hesapId
) {
}
