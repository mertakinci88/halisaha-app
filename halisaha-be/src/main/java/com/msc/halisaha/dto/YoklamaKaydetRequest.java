package com.msc.halisaha.dto;

import com.msc.halisaha.entity.YoklamaDurum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Seçili gün ve grup için tüm öğrencilerin yoklaması. Durumu null gönderilen öğrencinin kaydı (varsa) silinir. */
public record YoklamaKaydetRequest(
        @NotNull(message = "Yoklama listesi zorunludur") List<@Valid Kayit> kayitlar
) {

    public record Kayit(
            @NotNull(message = "Öğrenci zorunludur") Long ogrenciId,
            YoklamaDurum durum,
            @Size(max = 255, message = "Not en fazla 255 karakter olabilir") String not
    ) {
    }
}
