package com.msc.halisaha.controller;

import com.msc.halisaha.dto.EnvanterSetRequest;
import com.msc.halisaha.dto.EnvanterSetResponse;
import com.msc.halisaha.service.EnvanterSetService;
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
@RequestMapping("/api/envanter-setleri")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasAuthority('MODUL_ENVANTER')")
public class EnvanterSetController {

    private final EnvanterSetService setService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('MODUL_ENVANTER') or hasAuthority('MODUL_OGRENCI_ENVANTER')")
    public List<EnvanterSetResponse> list() {
        return setService.list();
    }

    @GetMapping("/{id}")
    public EnvanterSetResponse getById(@PathVariable Long id) {
        return setService.getById(id);
    }

    @PostMapping
    public ResponseEntity<EnvanterSetResponse> create(@Valid @RequestBody EnvanterSetRequest request) {
        return ResponseEntity.status(201).body(setService.create(request));
    }

    @PutMapping("/{id}")
    public EnvanterSetResponse update(@PathVariable Long id, @Valid @RequestBody EnvanterSetRequest request) {
        return setService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        setService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
