package com.msc.halisaha.service;

import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.dto.SahaRequest;
import com.msc.halisaha.dto.SahaResponse;
import com.msc.halisaha.entity.Saha;
import com.msc.halisaha.entity.SahaDurum;
import com.msc.halisaha.repository.RezervasyonRepository;
import com.msc.halisaha.repository.SahaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SahaService {

    private final SahaRepository sahaRepository;
    private final RezervasyonRepository rezervasyonRepository;

    public List<SahaResponse> list() {
        return sahaRepository.findAll().stream().map(SahaResponse::from).toList();
    }

    public SahaResponse getById(Long id) {
        return SahaResponse.from(getEntity(id));
    }

    @Transactional
    public SahaResponse create(SahaRequest request) {
        Saha saha = Saha.builder()
                .ad(request.ad())
                .durum(request.durum() != null ? request.durum() : SahaDurum.AKTIF)
                .saatlikUcret(request.saatlikUcret())
                .build();
        return SahaResponse.from(sahaRepository.save(saha));
    }

    @Transactional
    public SahaResponse update(Long id, SahaRequest request) {
        Saha saha = getEntity(id);
        saha.setAd(request.ad());
        saha.setSaatlikUcret(request.saatlikUcret());
        if (request.durum() != null) {
            saha.setDurum(request.durum());
        }
        return SahaResponse.from(saha);
    }

    @Transactional
    public void delete(Long id) {
        Saha saha = getEntity(id);
        if (rezervasyonRepository.existsBySahaId(id)) {
            saha.setDurum(SahaDurum.PASIF);
            return;
        }
        sahaRepository.delete(saha);
    }

    private Saha getEntity(Long id) {
        return sahaRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Saha bulunamadı: " + id));
    }
}
