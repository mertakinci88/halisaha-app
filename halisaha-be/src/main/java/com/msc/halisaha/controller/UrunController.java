package com.msc.halisaha.controller;

import com.msc.halisaha.dto.UrunRequest;
import com.msc.halisaha.dto.UrunResponse;
import com.msc.halisaha.service.UrunService;
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

@RestController
@RequestMapping("/api/urunler")
@RequiredArgsConstructor
public class UrunController {

    private final UrunService urunService;

    @GetMapping
    public List<UrunResponse> list() {
        return urunService.list();
    }

    @GetMapping("/{id}")
    public UrunResponse getById(@PathVariable Long id) {
        return urunService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('MODUL_URUN')")
    public ResponseEntity<UrunResponse> create(@Valid @RequestBody UrunRequest request) {
        return ResponseEntity.status(201).body(urunService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('MODUL_URUN')")
    public UrunResponse update(@PathVariable Long id, @Valid @RequestBody UrunRequest request) {
        return urunService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('MODUL_URUN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        urunService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
