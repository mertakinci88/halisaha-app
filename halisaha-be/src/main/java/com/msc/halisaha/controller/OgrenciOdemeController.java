package com.msc.halisaha.controller;

import com.msc.halisaha.dto.OgrenciOdemeRequest;
import com.msc.halisaha.dto.OgrenciOdemeYilResponse;
import com.msc.halisaha.service.OgrenciOdemeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Öğrenci detayındaki aylık ödemeleri (yıl bazında). */
@RestController
@RequestMapping("/api/ogrenciler/{ogrenciId}/odemeler")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasAuthority('MODUL_OGRENCI_ODEME')")
public class OgrenciOdemeController {

    private final OgrenciOdemeService odemeService;

    @GetMapping("/{yil}")
    public OgrenciOdemeYilResponse getir(@PathVariable Long ogrenciId, @PathVariable int yil) {
        return odemeService.getir(ogrenciId, yil);
    }

    /** Ayın yeni bir ödemesi (kısmi ya da tamamı). */
    @PostMapping("/{yil}/{ay}")
    public OgrenciOdemeYilResponse ekle(
            @PathVariable Long ogrenciId, @PathVariable int yil, @PathVariable int ay, @Valid @RequestBody OgrenciOdemeRequest request) {
        return odemeService.ekle(ogrenciId, yil, ay, request);
    }

    @PutMapping("/{yil}/{ay}/{odemeId}")
    public OgrenciOdemeYilResponse guncelle(
            @PathVariable Long ogrenciId, @PathVariable int yil, @PathVariable int ay, @PathVariable Long odemeId,
            @Valid @RequestBody OgrenciOdemeRequest request) {
        return odemeService.guncelle(ogrenciId, yil, ay, odemeId, request);
    }

    @DeleteMapping("/{yil}/{ay}/{odemeId}")
    public OgrenciOdemeYilResponse sil(@PathVariable Long ogrenciId, @PathVariable int yil, @PathVariable int ay, @PathVariable Long odemeId) {
        return odemeService.sil(ogrenciId, yil, ay, odemeId);
    }
}
