package com.msc.halisaha.controller;

import com.msc.halisaha.dto.AbonelikAyarRequest;
import com.msc.halisaha.dto.AbonelikAyarResponse;
import com.msc.halisaha.dto.AbonelikGuncelleRequest;
import com.msc.halisaha.dto.AbonelikOdemeRequest;
import com.msc.halisaha.dto.AbonelikOnizlemeRequest;
import com.msc.halisaha.dto.AbonelikOnizlemeResponse;
import com.msc.halisaha.dto.AbonelikRequest;
import com.msc.halisaha.dto.AbonelikIptalOnizlemeResponse;
import com.msc.halisaha.dto.AbonelikIptalRequest;
import com.msc.halisaha.dto.AbonelikResponse;
import com.msc.halisaha.entity.AbonelikDurum;
import com.msc.halisaha.service.AbonelikService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/abonelikler")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasAuthority('MODUL_ABONELIK')")
public class AbonelikController {

    private final AbonelikService abonelikService;

    @GetMapping
    public List<AbonelikResponse> listele(@RequestParam(required = false) AbonelikDurum durum, @RequestParam(required = false) String q) {
        return abonelikService.listele(durum, q);
    }

    /** Standart abonelik ücreti: kayıt formunda ücret alanını doldurur. */
    @GetMapping("/ayar")
    public AbonelikAyarResponse ayar() {
        return abonelikService.ayar();
    }

    /** Standart ücreti yalnızca yönetici değiştirir. */
    @PutMapping("/ayar")
    @PreAuthorize("hasRole('ADMIN')")
    public AbonelikAyarResponse ayarKaydet(@Valid @RequestBody AbonelikAyarRequest request) {
        return abonelikService.ayarKaydet(request);
    }

    @PostMapping("/onizleme")
    public AbonelikOnizlemeResponse onizle(@Valid @RequestBody AbonelikOnizlemeRequest request) {
        return abonelikService.onizle(request);
    }

    @GetMapping("/{id}")
    public AbonelikResponse getir(@PathVariable Long id) {
        return abonelikService.getir(id);
    }

    @PostMapping
    public ResponseEntity<AbonelikResponse> olustur(@Valid @RequestBody AbonelikRequest request) {
        return ResponseEntity.status(201).body(abonelikService.olustur(request));
    }

    @PutMapping("/{id}")
    public AbonelikResponse guncelle(@PathVariable Long id, @Valid @RequestBody AbonelikGuncelleRequest request) {
        return abonelikService.guncelle(id, request);
    }

    /** İptal ekranı için: kalan depozito, oynanacak/iptal edilecek hafta sayısı ve artan tutar. */
    @GetMapping("/{id}/iptal-onizleme")
    public AbonelikIptalOnizlemeResponse iptalOnizle(@PathVariable Long id) {
        return abonelikService.iptalOnizle(id);
    }

    @PutMapping("/{id}/iptal")
    public AbonelikResponse iptalEt(@PathVariable Long id, @Valid @RequestBody(required = false) AbonelikIptalRequest request) {
        return abonelikService.iptalEt(id, request != null ? request : new AbonelikIptalRequest(null, null, null, null));
    }

    @PostMapping("/{id}/odemeler")
    public ResponseEntity<AbonelikResponse> odemeEkle(@PathVariable Long id, @Valid @RequestBody AbonelikOdemeRequest request) {
        return ResponseEntity.status(201).body(abonelikService.odemeEkle(id, request));
    }

    @DeleteMapping("/{id}/odemeler/{odemeId}")
    public AbonelikResponse odemeSil(@PathVariable Long id, @PathVariable Long odemeId) {
        return abonelikService.odemeSil(id, odemeId);
    }
}
