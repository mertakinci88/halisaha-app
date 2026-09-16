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
        OdemeYontemi odemeYontemi,
        OdemeDurumu odemeDurumu,
        BigDecimal odenecekTutar,
        List<RezervasyonUrunResponse> urunler
) {
}
