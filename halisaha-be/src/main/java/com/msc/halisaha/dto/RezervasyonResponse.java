package com.msc.halisaha.dto;

import com.msc.halisaha.entity.OdemeDurumu;
import com.msc.halisaha.entity.OdemeYontemi;
import com.msc.halisaha.entity.RezervasyonDurum;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record RezervasyonResponse(
        Long id,
        Long sahaId,
        String sahaAd,
        String adSoyad,
        LocalDateTime baslangicTarih,
        LocalDateTime bitisTarih,
        RezervasyonDurum durum,
        String telNo,
        /** Son alınan ödemenin yöntemi. */
        OdemeYontemi odemeYontemi,
        OdemeDurumu odemeDurumu,
        /** Rezervasyonun sisteme girildiği tarih-saat; eski kayıtlarda null. */
        LocalDateTime kayitTarih,
        /** Rezervasyonu giren kullanıcının adı; eski kayıtlarda null. */
        String kaydedenAd,
        /** Tüm kalemlerin toplamı. */
        BigDecimal odenecekTutar,
        BigDecimal sahaTutar,
        BigDecimal odenenTutar,
        BigDecimal kalanTutar,
        BigDecimal kaporaTutar,
        List<RezervasyonUrunResponse> urunler,
        List<OdemeResponse> odemeler,
        /** Bir aboneliğin haftalık rezervasyonuysa o aboneliğin id'si; normal rezervasyonda null. */
        Long abonelikId
) {
}
