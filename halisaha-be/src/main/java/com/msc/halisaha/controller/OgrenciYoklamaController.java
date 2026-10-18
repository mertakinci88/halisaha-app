package com.msc.halisaha.controller;

import com.msc.halisaha.dto.OgrenciYoklamaSayfa;
import com.msc.halisaha.service.YoklamaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Öğrenci detayındaki yoklama bilgileri kartı. */
@RestController
@RequestMapping("/api/ogrenciler/{ogrenciId}/yoklamalar")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasAuthority('MODUL_OGRENCI_YOKLAMA')")
public class OgrenciYoklamaController {

    private final YoklamaService yoklamaService;

    /** Öğrencinin alınmış yoklamaları, tarihe göre yeniden eskiye; sayfalı. */
    @GetMapping
    public OgrenciYoklamaSayfa gecmis(
            @PathVariable Long ogrenciId,
            @RequestParam(defaultValue = "0") int sayfa,
            @RequestParam(defaultValue = "20") int boyut) {
        return yoklamaService.ogrenciGecmisi(ogrenciId, Math.max(0, sayfa), Math.min(Math.max(1, boyut), 100));
    }
}
