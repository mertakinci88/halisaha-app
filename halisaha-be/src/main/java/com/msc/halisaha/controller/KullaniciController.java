package com.msc.halisaha.controller;

import com.msc.halisaha.dto.KullaniciOlusturRequest;
import com.msc.halisaha.dto.KullaniciResponse;
import com.msc.halisaha.dto.YetkiGuncelleRequest;
import com.msc.halisaha.entity.KullaniciDurum;
import com.msc.halisaha.service.KullaniciService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Personel yönetimi: yalnızca ADMIN kullanıcıların personel hesapları ve menü yetkilerini yönetmesi içindir. */
@RestController
@RequestMapping("/api/kullanicilar")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class KullaniciController {

    private final KullaniciService kullaniciService;

    @GetMapping
    public List<KullaniciResponse> list() {
        return kullaniciService.list();
    }

    @GetMapping("/{id}")
    public KullaniciResponse getById(@PathVariable Long id) {
        return kullaniciService.getById(id);
    }

    @PostMapping
    public ResponseEntity<KullaniciResponse> create(@Valid @RequestBody KullaniciOlusturRequest request) {
        return ResponseEntity.status(201).body(kullaniciService.create(request));
    }

    @PutMapping("/{id}/yetkiler")
    public KullaniciResponse yetkileriGuncelle(@PathVariable Long id, @Valid @RequestBody YetkiGuncelleRequest request) {
        return kullaniciService.yetkileriGuncelle(id, request);
    }

    @PatchMapping("/{id}/durum")
    public KullaniciResponse durumGuncelle(@PathVariable Long id, @RequestParam KullaniciDurum durum) {
        return kullaniciService.durumGuncelle(id, durum);
    }
}
