package com.msc.halisaha.controller;

import com.msc.halisaha.dto.GrupRequest;
import com.msc.halisaha.dto.GrupResponse;
import com.msc.halisaha.service.GrupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/gruplar")
@RequiredArgsConstructor
public class GrupController {

    private final GrupService grupService;

    @GetMapping
    public List<GrupResponse> list() {
        return grupService.list();
    }

    @GetMapping("/{id}")
    public GrupResponse getById(@PathVariable Long id) {
        return grupService.getById(id);
    }

    @PostMapping
    public ResponseEntity<GrupResponse> create(@Valid @RequestBody GrupRequest request) {
        return ResponseEntity.status(201).body(grupService.create(request));
    }

    @PutMapping("/{id}")
    public GrupResponse update(@PathVariable Long id, @Valid @RequestBody GrupRequest request) {
        return grupService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        grupService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
