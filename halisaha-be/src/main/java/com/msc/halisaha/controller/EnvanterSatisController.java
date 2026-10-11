package com.msc.halisaha.controller;

import com.msc.halisaha.dto.EnvanterSatisRequest;
import com.msc.halisaha.dto.EnvanterSatisResponse;
import com.msc.halisaha.service.EnvanterSatisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Öğrenci detayındaki envanter seti satışları. */
@RestController
@RequestMapping("/api/ogrenciler/{ogrenciId}/envanter-satislari")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasAuthority('MODUL_ENVANTER')")
public class EnvanterSatisController {

    private final EnvanterSatisService satisService;

    @GetMapping
    public List<EnvanterSatisResponse> listele(@PathVariable Long ogrenciId) {
        return satisService.listele(ogrenciId);
    }

    @PostMapping
    public ResponseEntity<EnvanterSatisResponse> olustur(
            @PathVariable Long ogrenciId, @Valid @RequestBody EnvanterSatisRequest request) {
        return ResponseEntity.status(201).body(satisService.olustur(ogrenciId, request));
    }

    @PutMapping("/{satisId}")
    public EnvanterSatisResponse guncelle(
            @PathVariable Long ogrenciId, @PathVariable Long satisId, @Valid @RequestBody EnvanterSatisRequest request) {
        return satisService.guncelle(ogrenciId, satisId, request);
    }

    @DeleteMapping("/{satisId}")
    public ResponseEntity<Void> sil(@PathVariable Long ogrenciId, @PathVariable Long satisId) {
        satisService.sil(ogrenciId, satisId);
        return ResponseEntity.noContent().build();
    }
}
