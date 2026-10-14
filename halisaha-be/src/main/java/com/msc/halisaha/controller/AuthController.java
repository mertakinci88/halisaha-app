package com.msc.halisaha.controller;

import com.msc.halisaha.dto.KullaniciResponse;
import com.msc.halisaha.dto.LoginRequest;
import com.msc.halisaha.dto.RefreshRequest;
import com.msc.halisaha.dto.SifreDegistirRequest;
import com.msc.halisaha.dto.TokenResponse;
import com.msc.halisaha.common.security.IstemciBilgisi;
import com.msc.halisaha.common.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import com.msc.halisaha.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/me")
    public KullaniciResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return KullaniciResponse.from(principal.getKullanici());
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(authService.login(request, IstemciBilgisi.from(http)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request, HttpServletRequest http) {
        authService.logout(request, IstemciBilgisi.from(http));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sifre-degistir")
    public ResponseEntity<Void> sifreDegistir(@AuthenticationPrincipal UserPrincipal principal,
                                              @Valid @RequestBody SifreDegistirRequest request) {
        authService.sifreDegistir(principal.getId(), request);
        return ResponseEntity.noContent().build();
    }
}
