package com.msc.halisaha.service;

import com.msc.halisaha.common.security.UserPrincipal;
import com.msc.halisaha.entity.Kullanici;
import com.msc.halisaha.repository.KullaniciRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** İşlemi yapan oturumdaki kullanıcı (yoksa null). */
@Component
@RequiredArgsConstructor
public class MevcutKullanici {

    private final KullaniciRepository kullaniciRepository;

    public Kullanici getir() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return kullaniciRepository.getReferenceById(principal.getId());
        }
        return null;
    }
}
