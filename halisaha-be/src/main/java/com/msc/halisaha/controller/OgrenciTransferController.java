package com.msc.halisaha.controller;

import com.msc.halisaha.dto.TransferKararRequest;
import com.msc.halisaha.dto.TransferResponse;
import com.msc.halisaha.dto.TransferTalepRequest;
import com.msc.halisaha.entity.TransferDurum;
import com.msc.halisaha.service.OgrenciTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OgrenciTransferController {

    private final OgrenciTransferService transferService;

    /** Öğrencinin transfer geçmişi (öğrenci detayı). */
    @GetMapping("/ogrenciler/{ogrenciId}/transferler")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('MODUL_OGRENCI_TRANSFER')")
    public List<TransferResponse> gecmis(@PathVariable Long ogrenciId) {
        return transferService.ogrenciGecmisi(ogrenciId);
    }

    /** Transfer talebi açar; admin onaylayana kadar öğrenci eski grubunda kalır. */
    @PostMapping("/ogrenciler/{ogrenciId}/transferler")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('MODUL_OGRENCI_TRANSFER')")
    public ResponseEntity<TransferResponse> talepEt(@PathVariable Long ogrenciId, @Valid @RequestBody TransferTalepRequest request) {
        return ResponseEntity.status(201).body(transferService.talepEt(ogrenciId, request));
    }

    /** Bekleyen talebin hedef grubunu / açıklamasını günceller. */
    @PutMapping("/transferler/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('MODUL_OGRENCI_TRANSFER')")
    public TransferResponse guncelle(@PathVariable Long id, @Valid @RequestBody TransferTalepRequest request) {
        return transferService.guncelle(id, request);
    }

    /** Bekleyen talebi geri çeker. */
    @PutMapping("/transferler/{id}/iptal")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('MODUL_OGRENCI_TRANSFER')")
    public TransferResponse iptal(@PathVariable Long id, @Valid @RequestBody(required = false) TransferKararRequest request) {
        return transferService.iptalEt(id, request);
    }

    /** Transfer ekranı: durum boşsa tüm talepler. */
    @GetMapping("/transferler")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('MODUL_TRANSFER')")
    public List<TransferResponse> listele(@RequestParam(required = false) TransferDurum durum) {
        return transferService.listele(durum);
    }

    @GetMapping("/transferler/bekleyen-sayisi")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('MODUL_TRANSFER')")
    public Map<String, Long> bekleyenSayisi() {
        return Map.of("sayi", transferService.bekleyenSayisi());
    }

    @PutMapping("/transferler/{id}/onayla")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('MODUL_TRANSFER')")
    public TransferResponse onayla(@PathVariable Long id) {
        return transferService.onayla(id);
    }

    @PutMapping("/transferler/{id}/reddet")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('MODUL_TRANSFER')")
    public TransferResponse reddet(@PathVariable Long id, @Valid @RequestBody(required = false) TransferKararRequest request) {
        return transferService.reddet(id, request);
    }
}
