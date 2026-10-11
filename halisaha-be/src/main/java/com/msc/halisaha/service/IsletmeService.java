package com.msc.halisaha.service;

import com.msc.halisaha.common.TurkceSirala;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.dto.IsletmeOlusturRequest;
import com.msc.halisaha.dto.IsletmeRequest;
import com.msc.halisaha.dto.IsletmeResponse;
import com.msc.halisaha.dto.KullaniciOlusturRequest;
import com.msc.halisaha.entity.Isletme;
import com.msc.halisaha.entity.IsletmeDurum;
import com.msc.halisaha.entity.Rol;
import com.msc.halisaha.repository.IsletmeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** İşletme yönetimi (SUPER_ADMIN). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IsletmeService {

    private final IsletmeRepository isletmeRepository;
    private final KullaniciService kullaniciService;

    public List<IsletmeResponse> list() {
        return isletmeRepository.findAll().stream()
                .sorted(TurkceSirala.<Isletme>alfabetik(Isletme::getAd).thenComparing(Isletme::getId))
                .map(IsletmeResponse::from)
                .toList();
    }

    public IsletmeResponse getById(Long id) {
        return IsletmeResponse.from(getEntity(id));
    }

    @Transactional
    public IsletmeResponse create(IsletmeOlusturRequest request) {
        Isletme isletme = isletmeRepository.save(Isletme.builder()
                .ad(request.ad().trim())
                .yoneticiAdSoyad(bosIseNull(request.yoneticiAdSoyad()))
                .yoneticiTelNo(bosIseNull(request.yoneticiTelNo()))
                .build());
        IsletmeOlusturRequest.Yonetici y = request.yonetici();
        kullaniciService.create(isletme.getId(),
                new KullaniciOlusturRequest(y.kullaniciAdi(), y.sifre(), y.adSoyad(), Rol.ADMIN, null));
        return IsletmeResponse.from(isletme);
    }

    @Transactional
    public IsletmeResponse update(Long id, IsletmeRequest request) {
        Isletme isletme = getEntity(id);
        isletme.setAd(request.ad().trim());
        isletme.setYoneticiAdSoyad(bosIseNull(request.yoneticiAdSoyad()));
        isletme.setYoneticiTelNo(bosIseNull(request.yoneticiTelNo()));
        return IsletmeResponse.from(isletme);
    }

    /** Pasif işletmenin kullanıcıları giriş yapamaz ve mevcut oturumları kullanılamaz hale gelir. */
    @Transactional
    public IsletmeResponse durumGuncelle(Long id, IsletmeDurum durum) {
        Isletme isletme = getEntity(id);
        isletme.setDurum(durum);
        return IsletmeResponse.from(isletme);
    }

    private static String bosIseNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private Isletme getEntity(Long id) {
        return isletmeRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "İşletme bulunamadı: " + id));
    }
}
