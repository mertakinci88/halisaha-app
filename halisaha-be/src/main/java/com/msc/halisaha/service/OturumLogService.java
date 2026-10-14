package com.msc.halisaha.service;

import com.msc.halisaha.common.AppZaman;
import com.msc.halisaha.common.security.IstemciBilgisi;
import com.msc.halisaha.entity.Kullanici;
import com.msc.halisaha.entity.OturumLog;
import com.msc.halisaha.entity.OturumOlay;
import com.msc.halisaha.repository.OturumLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Giriş / çıkış geçmişi. Kayıt, çağıran işlem geri alınsa bile (ör. başarısız giriş) saklansın diye ayrı işlemde yazılır. */
@Service
@RequiredArgsConstructor
public class OturumLogService {

    private final OturumLogRepository oturumLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void kaydet(OturumOlay olay, Kullanici kullanici, String kullaniciAdi, IstemciBilgisi bilgi) {
        IstemciBilgisi b = bilgi != null ? bilgi : IstemciBilgisi.BILINMIYOR;
        String ad = kullanici != null ? kullanici.getKullaniciAdi() : kullaniciAdi;
        oturumLogRepository.save(OturumLog.builder()
                .isletmeId(kullanici != null && kullanici.getIsletme() != null ? kullanici.getIsletme().getId() : null)
                .kullaniciId(kullanici != null ? kullanici.getId() : null)
                .kullaniciAdi(ad != null && ad.length() > 100 ? ad.substring(0, 100) : ad == null ? "?" : ad)
                .olay(olay)
                .zaman(AppZaman.simdi())
                .ip(b.ip())
                .tarayici(b.tarayici())
                .build());
    }
}
