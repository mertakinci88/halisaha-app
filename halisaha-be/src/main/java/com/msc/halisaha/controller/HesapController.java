package com.msc.halisaha.controller;

import com.msc.halisaha.dto.HesapRequest;
import com.msc.halisaha.dto.HesapResponse;
import com.msc.halisaha.service.HesapService;
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

/**
 * Okuma, ödeme alan her kullanıcıya açıktır (Havale / EFT hesabı seçimi için);
 * tanımlama yalnızca ADMIN ya da HESAP yetkisi olanlara.
 */
@RestController
@RequestMapping("/api/hesaplar")
@RequiredArgsConstructor
public class HesapController {

    private final HesapService hesapService;

    @GetMapping
    public List<HesapResponse> list() {
        return hesapService.list();
    }

    @GetMapping("/{id}")
    public HesapResponse getById(@PathVariable Long id) {
        return hesapService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('MODUL_HESAP')")
    public ResponseEntity<HesapResponse> create(@Valid @RequestBody HesapRequest request) {
        return ResponseEntity.status(201).body(hesapService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('MODUL_HESAP')")
    public HesapResponse update(@PathVariable Long id, @Valid @RequestBody HesapRequest request) {
        return hesapService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('MODUL_HESAP')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        hesapService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
