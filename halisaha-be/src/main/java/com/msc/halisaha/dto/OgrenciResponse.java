package com.msc.halisaha.dto;

import com.msc.halisaha.entity.OgrenciOdemeDurumu;
import com.msc.halisaha.entity.Ogrenci;
import com.msc.halisaha.entity.OgrenciDurum;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record OgrenciResponse(
        Long id,
        Long grupId,
        String grupAd,
        String adSoyad,
        LocalDate dogumTarih,
        String veliAdSoyad,
        String veliTelNo,
        OgrenciDurum durum,
        Integer antrenmanGunSayisi,
        /** İçinde bulunulan ayın durumu: ödeme girilmişse ODENDI; girilmemişse ayın 15'ine kadar ODEME_BEKLIYOR, sonrasında ODENMEDI. */
        OgrenciOdemeDurumu odemeDurumu,
        LocalDateTime kayitTarih,
        String not,
        /** Kapalıysa öğrenciden ödeme alınmaz (odemeDurumu ODEMESIZ olur). */
        boolean odemeAlinir,
        /** Öğrencinin herhangi bir ödeme kaydı var mı (ödeme alımı kapatılırken uyarı için). */
        boolean odemeKaydiVar
) {
    public static OgrenciResponse from(Ogrenci ogrenci, OgrenciOdemeDurumu odemeDurumu, boolean odemeKaydiVar) {
        return new OgrenciResponse(
                ogrenci.getId(),
                ogrenci.getGrup() != null ? ogrenci.getGrup().getId() : null,
                ogrenci.getGrup() != null ? ogrenci.getGrup().getAd() : null,
                ogrenci.getAdSoyad(),
                ogrenci.getDogumTarih(),
                ogrenci.getVeliAdSoyad(),
                ogrenci.getVeliTelNo(),
                ogrenci.getDurum(),
                ogrenci.getAntrenmanGunSayisi(),
                ogrenci.isOdemeAlinir() ? odemeDurumu : OgrenciOdemeDurumu.ODEMESIZ,
                ogrenci.getKayitTarih(),
                ogrenci.getNot(),
                ogrenci.isOdemeAlinir(),
                odemeKaydiVar
        );
    }
}
