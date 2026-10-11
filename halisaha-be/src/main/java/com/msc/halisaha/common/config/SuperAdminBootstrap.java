package com.msc.halisaha.common.config;

import com.msc.halisaha.entity.Kullanici;
import com.msc.halisaha.entity.KullaniciDurum;
import com.msc.halisaha.entity.Rol;
import com.msc.halisaha.repository.KullaniciRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Sistemde hiç SUPER_ADMIN yoksa yapılandırmadaki bilgilerle bir tane oluşturur. Şifre migration
 * dosyasında tutulmaz; üretimde SUPER_ADMIN_PASSWORD ortam değişkeniyle verilir.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminBootstrap implements ApplicationRunner {

    private final KullaniciRepository kullaniciRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (kullaniciRepository.existsByRol(Rol.SUPER_ADMIN)) {
            return;
        }
        AppProperties.SuperAdmin ayar = appProperties.superAdmin();
        if (ayar == null || isBlank(ayar.username()) || isBlank(ayar.password())) {
            log.warn("Süper admin bulunamadı ve app.super-admin ayarı boş; süper admin oluşturulmadı");
            return;
        }
        if (kullaniciRepository.findByKullaniciAdi(ayar.username()).isPresent()) {
            log.warn("'{}' kullanıcı adı başka bir hesapta kullanılıyor; süper admin oluşturulmadı", ayar.username());
            return;
        }
        kullaniciRepository.save(Kullanici.builder()
                .kullaniciAdi(ayar.username())
                .sifre(passwordEncoder.encode(ayar.password()))
                .adSoyad(isBlank(ayar.adSoyad()) ? "Platform Yöneticisi" : ayar.adSoyad())
                .rol(Rol.SUPER_ADMIN)
                .durum(KullaniciDurum.AKTIF)
                .build());
        log.info("Süper admin oluşturuldu: {}", ayar.username());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
