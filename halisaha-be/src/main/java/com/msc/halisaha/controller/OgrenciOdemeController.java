package com.msc.halisaha.controller;

import com.msc.halisaha.dto.OgrenciOdemeKaydetRequest;
import com.msc.halisaha.dto.OgrenciOdemeYilResponse;
import com.msc.halisaha.service.OgrenciOdemeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Öğrenci detayındaki aylık ödemeleri (yıl bazında). */
@RestController
@RequestMapping("/api/ogrenciler/{ogrenciId}/odemeler")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasAuthority('MODUL_OGRENCI')")
public class OgrenciOdemeController {

    private final OgrenciOdemeService odemeService;

    @GetMapping("/{yil}")
    public OgrenciOdemeYilResponse getir(@PathVariable Long ogrenciId, @PathVariable int yil) {
        return odemeService.getir(ogrenciId, yil);
    }

    /** İstekte yer alan aylar kaydedilir; yer almayan aylara dokunulmaz. */
    @PutMapping("/{yil}")
    public OgrenciOdemeYilResponse kaydet(
            @PathVariable Long ogrenciId, @PathVariable int yil, @Valid @RequestBody OgrenciOdemeKaydetRequest request) {
        return odemeService.kaydet(ogrenciId, yil, request);
    }
}
