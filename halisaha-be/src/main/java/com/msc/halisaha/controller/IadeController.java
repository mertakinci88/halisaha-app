package com.msc.halisaha.controller;

import com.msc.halisaha.dto.IadeRequest;
import com.msc.halisaha.dto.IadeResponse;
import com.msc.halisaha.service.IadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** İadeleri her kullanıcı görür; iade yapmayı ve silmeyi yalnızca yönetici yapar (para çıkışı). */
@RestController
@RequestMapping("/api/iadeler")
@RequiredArgsConstructor
public class IadeController {

    private final IadeService iadeService;

    /** Tam olarak bir kaynak parametresi verilmelidir; öğrenci için {@code ogrenciId} ve {@code yil} verilerek yılın tüm iadeleri alınır. */
    @GetMapping
    public List<IadeResponse> liste(
            @RequestParam(required = false) Long ogrenciId,
            @RequestParam(required = false) Integer yil,
            @RequestParam(required = false) Long rezervasyonId,
            @RequestParam(required = false) Long ogrenciOdemeId,
            @RequestParam(required = false) Long envanterSatisId,
            @RequestParam(required = false) Long abonelikId) {
        if (ogrenciId != null && yil != null) {
            return iadeService.ogrenciYilListe(ogrenciId, yil);
        }
        return iadeService.liste(rezervasyonId, ogrenciOdemeId, envanterSatisId, abonelikId);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IadeResponse> yap(@Valid @RequestBody IadeRequest request) {
        return ResponseEntity.status(201).body(iadeService.yap(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sil(@PathVariable Long id) {
        iadeService.sil(id);
        return ResponseEntity.noContent().build();
    }
}
