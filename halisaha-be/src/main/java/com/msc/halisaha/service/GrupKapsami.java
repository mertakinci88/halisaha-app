package com.msc.halisaha.service;

import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.common.security.UserPrincipal;
import com.msc.halisaha.entity.Kullanici;
import com.msc.halisaha.entity.Ogrenci;
import com.msc.halisaha.entity.Rol;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Oturumdaki personelin hangi grupların öğrencilerini görebileceği. Yalnızca grup kısıtlı PERSONEL için
 * bir kısıt vardır; ADMIN ve kısıtsız personel tüm grupları görür. Kullanıcı her istekte veritabanından
 * yüklendiği için yetki değişikliği hemen geçerli olur.
 */
@Component
public class GrupKapsami {

    /** İzinli grup id'leri; kısıt yoksa null (tüm gruplar). Kısıtlıysa ve hiç grup seçilmediyse boş küme. */
    public Set<Long> izinli() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            Kullanici k = principal.getKullanici();
            if (k.getRol() == Rol.PERSONEL && k.isGrupKisitli()) {
                return Set.copyOf(k.getIzinliGruplar());
            }
        }
        return null;
    }

    public boolean kisitli() {
        return izinli() != null;
    }

    public boolean grupIzinli(Long grupId) {
        Set<Long> izinli = izinli();
        return izinli == null || (grupId != null && izinli.contains(grupId));
    }

    /** Öğrencinin grubu izinli değilse (ya da grupsuzsa) öğrenci hiç yokmuş gibi 404 döner. */
    public void kontrol(Ogrenci ogrenci) {
        if (!grupIzinli(ogrenci.getGrup() != null ? ogrenci.getGrup().getId() : null)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Öğrenci bulunamadı: " + ogrenci.getId());
        }
    }

    public void kontrolGrup(Long grupId) {
        if (!grupIzinli(grupId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Grup bulunamadı: " + grupId);
        }
    }
}
