package com.msc.halisaha.controller;

import com.msc.halisaha.dto.YoklamaKaydetRequest;
import com.msc.halisaha.dto.YoklamaSatir;
import com.msc.halisaha.service.YoklamaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/yoklamalar")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasAuthority('MODUL_YOKLAMA')")
public class YoklamaController {

    private final YoklamaService yoklamaService;

    /** Grubun aktif öğrencileri ve seçili günkü yoklamaları. */
    @GetMapping
    public List<YoklamaSatir> listele(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tarih,
            @RequestParam Long grupId) {
        return yoklamaService.listele(tarih, grupId);
    }

    /** Seçili gün ve grubun yoklamasını kaydeder; güncel listeyi döner. */
    @PutMapping
    public List<YoklamaSatir> kaydet(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tarih,
            @RequestParam Long grupId,
            @Valid @RequestBody YoklamaKaydetRequest request) {
        return yoklamaService.kaydet(tarih, grupId, request);
    }
}
