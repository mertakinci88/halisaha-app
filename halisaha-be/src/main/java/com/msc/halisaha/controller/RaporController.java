package com.msc.halisaha.controller;

import com.msc.halisaha.dto.CiroRaporu;
import com.msc.halisaha.service.RaporService;
import com.msc.halisaha.service.RaporService.Esas;
import com.msc.halisaha.service.RaporService.Grupla;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/raporlar")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RaporController {

    private final RaporService raporService;

    /**
     * Rezervasyon cirosu. Varsayılanlar: son 30 gün, günlük, tahsilat tarihine (ODEME) göre.
     * {@code esas=SEANS} seans tarihine göre, iptal edilmeyen rezervasyonların saha/ürün gelirini verir.
     */
    @GetMapping("/ciro")
    public CiroRaporu ciro(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslangic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitis,
            @RequestParam(required = false) Long sahaId,
            @RequestParam(defaultValue = "GUN") Grupla grupla,
            @RequestParam(defaultValue = "ODEME") Esas esas) {
        return raporService.ciro(baslangic, bitis, sahaId, grupla, esas);
    }
}
