package com.msc.halisaha.controller;

import com.msc.halisaha.dto.OgrenciRequest;
import com.msc.halisaha.dto.OgrenciFiltre;
import com.msc.halisaha.entity.EnvanterDurumFiltre;
import com.msc.halisaha.entity.EnvanterTur;
import com.msc.halisaha.entity.OgrenciDurum;
import com.msc.halisaha.entity.OgrenciKayitFiltre;
import com.msc.halisaha.entity.OgrenciOdemeFiltre;
import com.msc.halisaha.dto.OgrenciResponse;
import com.msc.halisaha.dto.PageResponse;
import com.msc.halisaha.service.OgrenciService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ogrenciler")
@RequiredArgsConstructor
public class OgrenciController {

    private final OgrenciService ogrenciService;

    /** durum: AKTIF | PASIF; odeme: ODENDI | BEKLEYEN | HIC | KISMI | GECIKMIS (içinde bulunulan ayın ödeme durumu); envanter: TESLIM_BEKLEYEN | BORCLU | ALMAMIS | TAMAM, envanterTur: envanter türü (ikisi birlikte ya da tek başına); kayit: BU_AY | SON_3_AY; antrenmanGun: haftalık antrenman günü; transferBekleyen: onay bekleyen grup değişikliği talebi; devamsizlik: son 30 günde en az bu kadar "Gelmedi" yoklaması; boş filtre uygulanmaz. */
    @GetMapping
    public PageResponse<OgrenciResponse> list(
            @RequestParam(required = false) Long grupId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) OgrenciDurum durum,
            @RequestParam(required = false) OgrenciOdemeFiltre odeme,
            @RequestParam(required = false) EnvanterDurumFiltre envanter,
            @RequestParam(required = false) EnvanterTur envanterTur,
            @RequestParam(required = false) OgrenciKayitFiltre kayit,
            @RequestParam(required = false) Integer antrenmanGun,
            @RequestParam(defaultValue = "false") boolean transferBekleyen,
            @RequestParam(required = false) Integer devamsizlik,
            @RequestParam(defaultValue = "0") int sayfa,
            @RequestParam(defaultValue = "10") int boyut) {
        return ogrenciService.list(new OgrenciFiltre(grupId, q, durum, odeme, envanter, envanterTur, kayit, antrenmanGun, transferBekleyen, devamsizlik), sayfa, boyut);
    }

    @GetMapping("/{id}")
    public OgrenciResponse getById(@PathVariable Long id) {
        return ogrenciService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('MODUL_OGRENCI')")
    public ResponseEntity<OgrenciResponse> create(@Valid @RequestBody OgrenciRequest request) {
        return ResponseEntity.status(201).body(ogrenciService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('MODUL_OGRENCI')")
    public OgrenciResponse update(@PathVariable Long id, @Valid @RequestBody OgrenciRequest request) {
        return ogrenciService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('MODUL_OGRENCI')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ogrenciService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
