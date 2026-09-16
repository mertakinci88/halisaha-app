package com.msc.halisaha.controller;

import com.msc.halisaha.dto.SahaRequest;
import com.msc.halisaha.dto.SahaResponse;
import com.msc.halisaha.service.SahaService;
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
@RequestMapping("/api/sahalar")
@RequiredArgsConstructor
public class SahaController {

    private final SahaService sahaService;

    @GetMapping
    public List<SahaResponse> list() {
        return sahaService.list();
    }

    @GetMapping("/{id}")
    public SahaResponse getById(@PathVariable Long id) {
        return sahaService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('MODUL_SAHA')")
    public ResponseEntity<SahaResponse> create(@Valid @RequestBody SahaRequest request) {
        return ResponseEntity.status(201).body(sahaService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('MODUL_SAHA')")
    public SahaResponse update(@PathVariable Long id, @Valid @RequestBody SahaRequest request) {
        return sahaService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('MODUL_SAHA')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sahaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
