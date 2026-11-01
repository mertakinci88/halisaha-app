package com.msc.halisaha.controller;

import com.msc.halisaha.dto.EnvanterTurRequest;
import com.msc.halisaha.dto.EnvanterTurResponse;
import com.msc.halisaha.service.EnvanterTurService;
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
@RequestMapping("/api/envanter-turleri")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasAuthority('MODUL_ENVANTER')")
public class EnvanterTurController {

    private final EnvanterTurService turService;

    /** Satış ekranları da türleri okuyabilsin diye öğrenci envanter yetkisi de listeleyebilir. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('MODUL_ENVANTER') or hasAuthority('MODUL_OGRENCI_ENVANTER')")
    public List<EnvanterTurResponse> list() {
        return turService.list();
    }

    @PostMapping
    public ResponseEntity<EnvanterTurResponse> create(@Valid @RequestBody EnvanterTurRequest request) {
        return ResponseEntity.status(201).body(turService.create(request));
    }

    @PutMapping("/{id}")
    public EnvanterTurResponse update(@PathVariable Long id, @Valid @RequestBody EnvanterTurRequest request) {
        return turService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        turService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
