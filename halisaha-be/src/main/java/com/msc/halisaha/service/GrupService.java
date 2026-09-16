package com.msc.halisaha.service;

import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.dto.GrupRequest;
import com.msc.halisaha.dto.GrupResponse;
import com.msc.halisaha.entity.Grup;
import com.msc.halisaha.repository.GrupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GrupService {

    private final GrupRepository grupRepository;

    public List<GrupResponse> list() {
        return grupRepository.findAll().stream().map(GrupResponse::from).toList();
    }

    public GrupResponse getById(Long id) {
        return GrupResponse.from(getEntity(id));
    }

    @Transactional
    public GrupResponse create(GrupRequest request) {
        Grup grup = Grup.builder().ad(request.ad()).build();
        return GrupResponse.from(grupRepository.save(grup));
    }

    @Transactional
    public GrupResponse update(Long id, GrupRequest request) {
        Grup grup = getEntity(id);
        grup.setAd(request.ad());
        return GrupResponse.from(grup);
    }

    @Transactional
    public void delete(Long id) {
        if (!grupRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Grup bulunamadı: " + id);
        }
        grupRepository.deleteById(id);
    }

    private Grup getEntity(Long id) {
        return grupRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Grup bulunamadı: " + id));
    }
}
