package com.msc.halisaha.service;

import com.msc.halisaha.common.TurkceSirala;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.dto.UrunRequest;
import com.msc.halisaha.dto.UrunResponse;
import com.msc.halisaha.entity.Urun;
import com.msc.halisaha.repository.UrunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UrunService {

    private final UrunRepository urunRepository;
    private final StokService stokService;

    public List<UrunResponse> list() {
        // Sıra sabit ve alfabetik olmalı: sıralamasız sorguda Postgres, güncellenen (stoğu değişen) satırı farklı yerde döndürebilir
        return urunRepository.findAll().stream()
                .sorted(TurkceSirala.<Urun>alfabetik(Urun::getAd).thenComparing(Urun::getId))
                .map(UrunResponse::from)
                .toList();
    }

    public UrunResponse getById(Long id) {
        return UrunResponse.from(getEntity(id));
    }

    @Transactional
    public UrunResponse create(UrunRequest request) {
        Urun urun = Urun.builder()
                .ad(request.ad())
                .adet(request.adet())
                .stokUyariAdedi(request.stokUyariAdedi() != null ? request.stokUyariAdedi() : 10)
                .fiyat(request.fiyat())
                .build();
        return UrunResponse.from(urunRepository.save(urun));
    }

    @Transactional
    public UrunResponse update(Long id, UrunRequest request) {
        Urun urun = getEntity(id);
        int eskiAdet = urun.getAdet();
        urun.setAd(request.ad());
        urun.setAdet(request.adet());
        if (request.stokUyariAdedi() != null) {
            urun.setStokUyariAdedi(request.stokUyariAdedi());
        }
        stokService.duzelt(urun, eskiAdet, request.adet());
        urun.setFiyat(request.fiyat());
        return UrunResponse.from(urun);
    }

    @Transactional
    public void delete(Long id) {
        if (!urunRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Ürün bulunamadı: " + id);
        }
        urunRepository.deleteById(id);
    }

    private Urun getEntity(Long id) {
        return urunRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ürün bulunamadı: " + id));
    }
}
