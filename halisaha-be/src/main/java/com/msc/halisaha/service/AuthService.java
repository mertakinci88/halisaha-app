package com.msc.halisaha.service;

import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.common.security.JwtService;
import com.msc.halisaha.common.security.UserPrincipal;
import com.msc.halisaha.dto.LoginRequest;
import com.msc.halisaha.dto.RefreshRequest;
import com.msc.halisaha.dto.TokenResponse;
import com.msc.halisaha.entity.Kullanici;
import com.msc.halisaha.entity.RefreshToken;
import com.msc.halisaha.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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

    @Transactional
    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.kullaniciAdi(), request.sifre()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
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
        String newAccessToken = jwtService.generateAccessToken(new UserPrincipal(kullanici));

        return new TokenResponse(newAccessToken, stored.getToken(), TOKEN_TYPE);
    }

    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokenRepository.findByToken(request.refreshToken())
                .ifPresent(refreshToken -> {
                    refreshToken.setRevoked(true);
                    refreshTokenRepository.save(refreshToken);
                });
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
