package com.msc.halisaha.controller;

import com.msc.halisaha.dto.EnvanterRequest;
import com.msc.halisaha.dto.EnvanterResponse;
import com.msc.halisaha.dto.EnvanterTeslimBekleyenResponse;
import com.msc.halisaha.service.EnvanterSatisService;
import com.msc.halisaha.service.EnvanterService;
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
@RequestMapping("/api/envanterler")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasAuthority('MODUL_ENVANTER')")
public class EnvanterController {

    private final EnvanterService envanterService;
    private final EnvanterSatisService satisService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('MODUL_ENVANTER') or hasAuthority('MODUL_OGRENCI_ENVANTER')")
    public List<EnvanterResponse> list() {
        return envanterService.list();
    }

    /** Panel kartı: ödemesi alınmış ama teslim edilmemiş envanterler. */
    @GetMapping("/teslim-bekleyenler")
    public EnvanterTeslimBekleyenResponse teslimBekleyenler() {
        return satisService.teslimBekleyenler();
    }

    @GetMapping("/{id}")
    public EnvanterResponse getById(@PathVariable Long id) {
        return envanterService.getById(id);
    }

    @PostMapping
    public ResponseEntity<EnvanterResponse> create(@Valid @RequestBody EnvanterRequest request) {
        return ResponseEntity.status(201).body(envanterService.create(request));
    }

    @PutMapping("/{id}")
    public EnvanterResponse update(@PathVariable Long id, @Valid @RequestBody EnvanterRequest request) {
        return envanterService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        envanterService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
