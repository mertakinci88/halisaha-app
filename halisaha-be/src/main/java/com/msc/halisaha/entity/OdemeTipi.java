package com.msc.halisaha.entity;

public enum OdemeTipi {
    /** Rezervasyon için alınan ilk kısmi ödeme; rezervasyon başına yalnızca bir kez alınır. */
    KAPORA,
    /** Kaporadan sonra alınan kısmi ödemeler. */
    KISMI,
    /** Kalan tutarın tamamını kapatan ödeme. */
    ODEME
}
