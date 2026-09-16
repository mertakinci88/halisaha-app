package com.msc.halisaha.controller;

import com.msc.halisaha.dto.RezervasyonRequest;
import com.msc.halisaha.dto.RezervasyonResponse;
import com.msc.halisaha.dto.RezervasyonUrunRequest;
import com.msc.halisaha.service.RezervasyonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rezervasyonlar")
@RequiredArgsConstructor
public class RezervasyonController {

    private final RezervasyonService rezervasyonService;

    @GetMapping
    public List<RezervasyonResponse> list(
            @RequestParam(required = false) Long sahaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tarih) {
        return rezervasyonService.list(sahaId, tarih);
    }

    @GetMapping("/{id}")
    public RezervasyonResponse getById(@PathVariable Long id) {
        return rezervasyonService.getById(id);
    }

    @PostMapping
    public ResponseEntity<RezervasyonResponse> create(@Valid @RequestBody RezervasyonRequest request) {
        return ResponseEntity.status(201).body(rezervasyonService.create(request));
    }

    @PutMapping("/{id}")
    public RezervasyonResponse update(@PathVariable Long id, @Valid @RequestBody RezervasyonRequest request) {
        return rezervasyonService.update(id, request);
    }

    @PatchMapping("/{id}/iptal")
    public RezervasyonResponse cancel(@PathVariable Long id) {
        return rezervasyonService.cancel(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        rezervasyonService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/urunler")
    public RezervasyonResponse urunEkle(@PathVariable Long id, @Valid @RequestBody RezervasyonUrunRequest request) {
        return rezervasyonService.urunEkle(id, request);
    }

    @DeleteMapping("/{id}/urunler/{rezervasyonUrunId}")
    public RezervasyonResponse urunCikar(@PathVariable Long id, @PathVariable Long rezervasyonUrunId) {
        return rezervasyonService.urunCikar(id, rezervasyonUrunId);
    }
}
