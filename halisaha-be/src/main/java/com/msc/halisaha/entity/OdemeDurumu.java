package com.msc.halisaha.entity;

import java.math.BigDecimal;

public enum OdemeDurumu {
    ODENDI,
    ODENMEDI,
    KISMI_ODENDI;

    /** Rezervasyonun ödeme durumu, alınan ödemelerin toplamından türetilir. */
    public static OdemeDurumu hesapla(BigDecimal odenen, BigDecimal toplam) {
        if (odenen.signum() <= 0) {
            return ODENMEDI;
        }
        return odenen.compareTo(toplam) >= 0 ? ODENDI : KISMI_ODENDI;
    }
}
