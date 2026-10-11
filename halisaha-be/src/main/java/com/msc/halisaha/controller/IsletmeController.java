package com.msc.halisaha.controller;

import com.msc.halisaha.dto.IsletmeOlusturRequest;
import com.msc.halisaha.dto.IsletmeRequest;
import com.msc.halisaha.dto.IsletmeResponse;
import com.msc.halisaha.dto.KullaniciOlusturRequest;
import com.msc.halisaha.dto.KullaniciResponse;
import com.msc.halisaha.dto.SifreSifirlaRequest;
import com.msc.halisaha.entity.IsletmeDurum;
import com.msc.halisaha.entity.KullaniciDurum;
import com.msc.halisaha.service.IsletmeService;
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

/** Platform yönetimi: işletmeler ve işletme kullanıcıları yalnızca SUPER_ADMIN tarafından yönetilir. */
@RestController
@RequestMapping("/api/isletmeler")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class IsletmeController {

    private final IsletmeService isletmeService;
    private final KullaniciService kullaniciService;

    @GetMapping
    public List<IsletmeResponse> list() {
        return isletmeService.list();
    }

    @GetMapping("/{id}")
    public IsletmeResponse getById(@PathVariable Long id) {
        return isletmeService.getById(id);
    }

    @PostMapping
    public ResponseEntity<IsletmeResponse> create(@Valid @RequestBody IsletmeOlusturRequest request) {
        return ResponseEntity.status(201).body(isletmeService.create(request));
    }

    @PutMapping("/{id}")
    public IsletmeResponse update(@PathVariable Long id, @Valid @RequestBody IsletmeRequest request) {
        return isletmeService.update(id, request);
    }

    @PatchMapping("/{id}/durum")
    public IsletmeResponse durumGuncelle(@PathVariable Long id, @RequestParam IsletmeDurum durum) {
        return isletmeService.durumGuncelle(id, durum);
    }

    @GetMapping("/{id}/kullanicilar")
    public List<KullaniciResponse> kullanicilar(@PathVariable Long id) {
        isletmeService.getById(id);
        return kullaniciService.list(id);
    }

    @PostMapping("/{id}/kullanicilar")
    public ResponseEntity<KullaniciResponse> kullaniciOlustur(
            @PathVariable Long id, @Valid @RequestBody KullaniciOlusturRequest request) {
        return ResponseEntity.status(201).body(kullaniciService.create(id, request));
    }

    @PatchMapping("/{id}/kullanicilar/{kullaniciId}/durum")
    public KullaniciResponse kullaniciDurum(
            @PathVariable Long id, @PathVariable Long kullaniciId, @RequestParam KullaniciDurum durum) {
        return kullaniciService.durumGuncelle(id, kullaniciId, durum);
    }

    @PutMapping("/{id}/kullanicilar/{kullaniciId}/sifre")
    public ResponseEntity<Void> sifreSifirla(
            @PathVariable Long id, @PathVariable Long kullaniciId, @Valid @RequestBody SifreSifirlaRequest request) {
        kullaniciService.sifreSifirla(id, kullaniciId, request.sifre());
        return ResponseEntity.noContent().build();
    }
}
