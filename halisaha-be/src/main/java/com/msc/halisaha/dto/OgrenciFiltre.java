package com.msc.halisaha.dto;

import com.msc.halisaha.entity.EnvanterDurumFiltre;
import com.msc.halisaha.entity.EnvanterTur;
import com.msc.halisaha.entity.OgrenciDurum;
import com.msc.halisaha.entity.OgrenciKayitFiltre;
import com.msc.halisaha.entity.OgrenciOdemeFiltre;

/**
 * Öğrenci listesi filtreleri; null / false olan filtre uygulanmaz.
 * envanterTur tek başına seçilirse o türden kalemi olan (satın almış) öğrenciler gelir; envanter durumuyla birlikte
 * durum o türün kalemlerine daraltılır (TAMAM hariç: tamamlanma tüm satışlar için aranır, türden kalemi olması da şarttır).
 * antrenmanGun: haftalık antrenman günü sayısı; transferBekleyen: onay bekleyen grup değişikliği talebi olanlar;
 * devamsizlik: son 30 günde en az bu kadar "Gelmedi" yoklaması olanlar (izinli sayılmaz).
 */
public record OgrenciFiltre(
        Long grupId,
        String q,
        OgrenciDurum durum,
        OgrenciOdemeFiltre odeme,
        EnvanterDurumFiltre envanter,
        EnvanterTur envanterTur,
        OgrenciKayitFiltre kayit,
        Integer antrenmanGun,
        boolean transferBekleyen,
        Integer devamsizlik
) {

    /** Eski biçim: envanterEksik = envanter durumu TESLIM_BEKLEYEN. */
    public OgrenciFiltre(Long grupId, String q, OgrenciDurum durum, OgrenciOdemeFiltre odeme, boolean envanterEksik) {
        this(grupId, q, durum, odeme, envanterEksik ? EnvanterDurumFiltre.TESLIM_BEKLEYEN : null, null);
    }

    /** Yalnızca grup, arama, durum, ödeme ve envanter filtreleriyle. */
    public OgrenciFiltre(Long grupId, String q, OgrenciDurum durum, OgrenciOdemeFiltre odeme,
                         EnvanterDurumFiltre envanter, EnvanterTur envanterTur) {
        this(grupId, q, durum, odeme, envanter, envanterTur, null, null, false, null);
    }
}
