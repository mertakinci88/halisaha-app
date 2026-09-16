package com.msc.halisaha.service;

import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.dto.KullaniciOlusturRequest;
import com.msc.halisaha.dto.KullaniciResponse;
import com.msc.halisaha.dto.YetkiGuncelleRequest;
import com.msc.halisaha.entity.Kullanici;
import com.msc.halisaha.entity.KullaniciDurum;
import com.msc.halisaha.entity.Modul;
import com.msc.halisaha.entity.Rol;
import com.msc.halisaha.repository.KullaniciRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KullaniciService {

    private final KullaniciRepository kullaniciRepository;
    private final PasswordEncoder passwordEncoder;

    public List<KullaniciResponse> list() {
        return kullaniciRepository.findAll().stream().map(KullaniciResponse::from).toList();
    }

    public KullaniciResponse getById(Long id) {
        return KullaniciResponse.from(getEntity(id));
    }

    @Transactional
    public KullaniciResponse create(KullaniciOlusturRequest request) {
        Kullanici kullanici = Kullanici.builder()
                .kullaniciAdi(request.kullaniciAdi())
                .sifre(passwordEncoder.encode(request.sifre()))
                .adSoyad(request.adSoyad())
                .rol(request.rol())
                .durum(KullaniciDurum.AKTIF)
                .yetkiler(request.rol() == Rol.ADMIN || request.yetkiler() == null
                        ? new HashSet<>()
                        : new HashSet<>(request.yetkiler()))
                .build();
        return KullaniciResponse.from(kullaniciRepository.save(kullanici));
    }

    @Transactional
    public KullaniciResponse yetkileriGuncelle(Long id, YetkiGuncelleRequest request) {
        Kullanici kullanici = getEntity(id);
        if (kullanici.getRol() == Rol.ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Admin kullanıcısının yetkileri sınırlandırılamaz");
        }
        Set<Modul> yeniYetkiler = kullanici.getYetkiler();
        yeniYetkiler.clear();
        yeniYetkiler.addAll(request.yetkiler());
        return KullaniciResponse.from(kullanici);
    }

    @Transactional
    public KullaniciResponse durumGuncelle(Long id, KullaniciDurum durum) {
        Kullanici kullanici = getEntity(id);
        boolean sonAktifAdmin = kullanici.getRol() == Rol.ADMIN
                && durum == KullaniciDurum.PASIF
                && kullaniciRepository.countByRolAndDurum(Rol.ADMIN, KullaniciDurum.AKTIF) <= 1;
        if (sonAktifAdmin) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Sistemdeki son aktif admin pasif hale getirilemez");
        }
        kullanici.setDurum(durum);
        return KullaniciResponse.from(kullanici);
    }

    private Kullanici getEntity(Long id) {
        return kullaniciRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı: " + id));
    }
}
