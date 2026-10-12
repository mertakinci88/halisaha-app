package com.msc.halisaha.entity;

public enum OdemeTipi {
    /** Yalnızca rezervasyon kaydı sırasında peşin alınan tutar; sonradan KAPORA tipinde ödeme alınamaz. */
    KAPORA,
    /** Rezervasyon kaydından sonra alınan kısmi ödemeler (kapora alınmış olması gerekmez). */
    KISMI,
    /** Kalan tutarın tamamını kapatan ödeme. */
    ODEME
}
