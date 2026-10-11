package com.msc.halisaha.common.security;

import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.entity.Isletme;
import com.msc.halisaha.repository.IsletmeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * İsteği yapan kullanıcının işletmesi. İşletme bilgisi istekten (parametre/gövde) asla alınmaz;
 * her istekte veritabanından yüklenen oturum kullanıcısından okunur, böylece bir işletme
 * başka işletmenin verisine erişemez.
 */
@Component
@RequiredArgsConstructor
public class IsletmeContext {

    private final IsletmeRepository isletmeRepository;

    /** Yeni kayıtlara bağlanacak işletme referansı (sorgu atmaz). */
    public Isletme getir() {
        return isletmeRepository.getReferenceById(id());
    }

    /** Oturumdaki kullanıcının işletme id'si; işletmeye bağlı değilse (SUPER_ADMIN) 403. */
    public Long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal && principal.getIsletmeId() != null) {
            return principal.getIsletmeId();
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "Bu işlem için bir işletmeye bağlı olmalısınız");
    }
}
