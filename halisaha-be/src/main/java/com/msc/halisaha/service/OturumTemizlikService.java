package com.msc.halisaha.service;

import com.msc.halisaha.common.AppZaman;
import com.msc.halisaha.entity.Kullanici;
import com.msc.halisaha.entity.OturumLog;
import com.msc.halisaha.entity.OturumOlay;
import com.msc.halisaha.entity.RefreshToken;
import com.msc.halisaha.repository.OturumLogRepository;
import com.msc.halisaha.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Süresi dolan oturumları giriş geçmişine "oturum süresi doldu" olarak yazar ve eski kayıtları siler,
 * böylece refresh_token ve oturum_log tabloları sınırsız büyümez.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OturumTemizlikService {

    /** Süresi dolmuş refresh token'lar bu kadar gün sonra silinir (öncesinde olayı yazılmış olur). */
    static final int TOKEN_SAKLAMA_GUN = 30;
    /** Giriş geçmişi bu kadar gün tutulur. */
    static final int LOG_SAKLAMA_GUN = 365;
    private static final int SAYFA = 500;

    private final RefreshTokenRepository refreshTokenRepository;
    private final OturumLogRepository oturumLogRepository;

    /** Her saat başı; sunucu kapalıyken kaçan süreler bir sonraki çalışmada bitiş zamanlarıyla yazılır. */
    @Scheduled(cron = "0 5 * * * *", zone = "Europe/Istanbul")
    public void zamanlanmisCalistir() {
        try {
            temizle(Instant.now());
        } catch (RuntimeException e) {
            log.warn("Oturum temizliği başarısız oldu", e);
        }
    }

    @Transactional
    public Sonuc temizle(Instant simdi) {
        int kapanan = 0;
        List<RefreshToken> sayfa;
        while (!(sayfa = refreshTokenRepository.suresiDolanlar(simdi, PageRequest.of(0, SAYFA))).isEmpty()) {
            for (RefreshToken t : sayfa) {
                Kullanici k = t.getKullanici();
                oturumLogRepository.save(OturumLog.builder()
                        .isletmeId(k.getIsletme() != null ? k.getIsletme().getId() : null)
                        .kullaniciId(k.getId())
                        .kullaniciAdi(k.getKullaniciAdi())
                        .olay(OturumOlay.OTURUM_SURESI_DOLDU)
                        .zaman(LocalDateTime.ofInstant(t.getExpiryDate(), AppZaman.ISLETME))
                        .build());
                t.setRevoked(true);
            }
            refreshTokenRepository.saveAllAndFlush(sayfa);
            kapanan += sayfa.size();
        }
        int silinenToken = refreshTokenRepository.eskileriSil(simdi.minus(java.time.Duration.ofDays(TOKEN_SAKLAMA_GUN)));
        int silinenLog = oturumLogRepository.eskileriSil(LocalDateTime.ofInstant(simdi, AppZaman.ISLETME).minusDays(LOG_SAKLAMA_GUN));
        if (kapanan + silinenToken + silinenLog > 0) {
            log.info("Oturum temizliği: {} oturum süresi doldu, {} eski token ve {} eski giriş kaydı silindi", kapanan, silinenToken, silinenLog);
        }
        return new Sonuc(kapanan, silinenToken, silinenLog);
    }

    public record Sonuc(int kapananOturum, int silinenToken, int silinenLog) {
    }
}
