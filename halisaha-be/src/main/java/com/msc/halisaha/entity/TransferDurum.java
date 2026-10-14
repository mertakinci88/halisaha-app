package com.msc.halisaha.entity;

public enum TransferDurum {
    BEKLIYOR,
    ONAYLANDI,
    REDDEDILDI,
    /** Talebi açan (ya da admin) onaydan önce geri çekti. */
    IPTAL
}
