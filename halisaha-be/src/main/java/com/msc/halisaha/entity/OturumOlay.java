package com.msc.halisaha.entity;

public enum OturumOlay {
    GIRIS,
    CIKIS,
    BASARISIZ_GIRIS,
    /** Çıkış yapılmadan oturumun geçerlilik süresi doldu; zaman olarak süresinin bittiği an yazılır. */
    OTURUM_SURESI_DOLDU,
    /** Çıkış yapılmadan tarayıcı kapatıldı (uzun süre sinyal gelmedi); zaman olarak son görülme anı yazılır. */
    TARAYICI_KAPATILDI,
    /** Tarayıcı kapatıldı kaydından sonra aynı oturumla geri dönüldü. */
    OTURUMA_DONDU
}
