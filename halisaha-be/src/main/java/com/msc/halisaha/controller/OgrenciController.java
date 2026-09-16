package com.msc.halisaha.controller;

import com.msc.halisaha.dto.OgrenciRequest;
import com.msc.halisaha.dto.OgrenciResponse;
import com.msc.halisaha.service.OgrenciService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ogrenciler")
@RequiredArgsConstructor
public class OgrenciController {

    private final OgrenciService ogrenciService;

    @GetMapping
    public List<OgrenciResponse> list(@RequestParam(required = false) Long grupId) {
        return ogrenciService.list(grupId);
    }

    @GetMapping("/{id}")
    public OgrenciResponse getById(@PathVariable Long id) {
        return ogrenciService.getById(id);
    }

    @PostMapping
    public ResponseEntity<OgrenciResponse> create(@Valid @RequestBody OgrenciRequest request) {
        return ResponseEntity.status(201).body(ogrenciService.create(request));
    }

    @PutMapping("/{id}")
    public OgrenciResponse update(@PathVariable Long id, @Valid @RequestBody OgrenciRequest request) {
        return ogrenciService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ogrenciService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
