package com.msc.halisaha.service;

import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.entity.OturumOlay;
import com.msc.halisaha.common.security.IstemciBilgisi;
import com.msc.halisaha.common.security.JwtService;
import com.msc.halisaha.common.security.UserPrincipal;
import com.msc.halisaha.dto.LoginRequest;
import com.msc.halisaha.dto.RefreshRequest;
import com.msc.halisaha.dto.SifreDegistirRequest;
import com.msc.halisaha.dto.TokenResponse;
import com.msc.halisaha.entity.Kullanici;
import com.msc.halisaha.entity.RefreshToken;
import com.msc.halisaha.repository.KullaniciRepository;
import com.msc.halisaha.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final KullaniciRepository kullaniciRepository;
    private final PasswordEncoder passwordEncoder;
    private final OturumLogService oturumLogService;

    public TokenResponse login(LoginRequest request) {
        return login(request, IstemciBilgisi.BILINMIYOR);
    }

    /** Başarılı ve başarısız girişler, istemci adresiyle birlikte giriş geçmişine yazılır. */
    @Transactional
    public TokenResponse login(LoginRequest request, IstemciBilgisi bilgi) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.kullaniciAdi(), request.sifre()));
        } catch (AuthenticationException e) {
            // Kullanıcı biliniyorsa (yanlış şifre / pasif hesap) kendisine bağlanır; bilinmiyorsa yalnızca girilen ad tutulur
            oturumLogService.kaydet(OturumOlay.BASARISIZ_GIRIS,
                    kullaniciRepository.findByKullaniciAdi(request.kullaniciAdi()).orElse(null), request.kullaniciAdi(), bilgi);
            throw e;
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        oturumLogService.kaydet(OturumOlay.GIRIS, principal.getKullanici(), request.kullaniciAdi(), bilgi);
        return issueTokenPair(principal.getKullanici());
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Geçersiz refresh token"));

        if (stored.isRevoked() || stored.getExpiryDate().isBefore(Instant.now()) || !jwtService.isTokenValid(stored.getToken())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token geçersiz veya süresi dolmuş");
        }

        Kullanici kullanici = stored.getKullanici();
        UserPrincipal principal = new UserPrincipal(kullanici);
        if (!principal.isEnabled()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Hesap veya işletme pasif durumda");
        }
        String newAccessToken = jwtService.generateAccessToken(principal);

        return new TokenResponse(newAccessToken, stored.getToken(), TOKEN_TYPE);
    }

    public void logout(RefreshRequest request) {
        logout(request, IstemciBilgisi.BILINMIYOR);
    }

    /** Çıkış, geçerli bir oturum kapatıldıysa giriş geçmişine yazılır (aynı oturum için tekrar çıkışlar yazılmaz). */
    @Transactional
    public void logout(RefreshRequest request, IstemciBilgisi bilgi) {
        refreshTokenRepository.findByToken(request.refreshToken())
                .ifPresent(refreshToken -> {
                    boolean aktifti = !refreshToken.isRevoked();
                    refreshToken.setRevoked(true);
                    refreshTokenRepository.save(refreshToken);
                    if (aktifti) {
                        oturumLogService.kaydet(OturumOlay.CIKIS, refreshToken.getKullanici(), null, bilgi);
                    }
                });
    }

    /**
     * Kullanıcı kendi şifresini mevcut şifresini doğrulayarak değiştirir. Hata 400 döner (401 değil):
     * istemci 401'i oturum süresi doldu sanıp kullanıcıyı çıkışa yönlendirir.
     * Başarılı olunca tüm refresh token'lar iptal edilir; kullanıcı yeniden giriş yapar.
     */
    @Transactional
    public void sifreDegistir(Long kullaniciId, SifreDegistirRequest request) {
        Kullanici kullanici = kullaniciRepository.findById(kullaniciId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));
        if (!passwordEncoder.matches(request.eskiSifre(), kullanici.getSifre())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mevcut şifre hatalı");
        }
        if (request.eskiSifre().equals(request.yeniSifre())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Yeni şifre mevcut şifreyle aynı olamaz");
        }
        kullanici.setSifre(passwordEncoder.encode(request.yeniSifre()));
        refreshTokenRepository.tumunuIptalEt(kullanici.getId());
    }

    private TokenResponse issueTokenPair(Kullanici kullanici) {
        UserPrincipal principal = new UserPrincipal(kullanici);
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);

        RefreshToken entity = RefreshToken.builder()
                .kullanici(kullanici)
                .token(refreshToken)
                .expiryDate(Instant.now().plusMillis(jwtService.getRefreshTokenExpirationMs()))
                .revoked(false)
                .build();
        refreshTokenRepository.save(entity);

        return new TokenResponse(accessToken, refreshToken, TOKEN_TYPE);
    }
}
