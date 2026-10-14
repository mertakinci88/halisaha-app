package com.msc.halisaha.service;

import com.msc.halisaha.common.TurkceSirala;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.common.security.IsletmeContext;
import com.msc.halisaha.dto.HesapRequest;
import com.msc.halisaha.dto.HesapResponse;
import com.msc.halisaha.entity.Hesap;
import com.msc.halisaha.entity.HesapDurum;
import com.msc.halisaha.entity.OdemeYontemi;
import com.msc.halisaha.repository.HesapRepository;
import com.msc.halisaha.repository.AbonelikOdemeRepository;
import com.msc.halisaha.repository.EnvanterSatisOdemeRepository;
import com.msc.halisaha.repository.IadeRepository;
import com.msc.halisaha.repository.RezervasyonOdemeRepository;
import com.msc.halisaha.repository.OgrenciOdemeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HesapService {

    private final HesapRepository hesapRepository;
    private final RezervasyonOdemeRepository rezervasyonOdemeRepository;
    private final EnvanterSatisOdemeRepository envanterSatisOdemeRepository;
    private final AbonelikOdemeRepository abonelikOdemeRepository;
    private final IadeRepository iadeRepository;
    private final OgrenciOdemeRepository ogrenciOdemeRepository;
    private final IsletmeContext isletmeContext;

    public List<HesapResponse> list() {
        return hesapRepository.findByIsletmeId(isletmeContext.id()).stream()
                .sorted(TurkceSirala.<Hesap>alfabetik(Hesap::getAd).thenComparing(Hesap::getId))
                .map(HesapResponse::from)
                .toList();
    }

    public HesapResponse getById(Long id) {
        return HesapResponse.from(getEntity(id));
    }

    @Transactional
    public HesapResponse create(HesapRequest request) {
        Hesap hesap = Hesap.builder()
                .isletme(isletmeContext.getir())
                .ad(request.ad().trim())
                .iban(ibanNormalle(request.iban()))
                .hesapSahibi(request.hesapSahibi().trim())
                .durum(request.durum() != null ? request.durum() : HesapDurum.AKTIF)
                .build();
        return HesapResponse.from(hesapRepository.save(hesap));
    }

    @Transactional
    public HesapResponse update(Long id, HesapRequest request) {
        Hesap hesap = getEntity(id);
        hesap.setAd(request.ad().trim());
        hesap.setIban(ibanNormalle(request.iban()));
        hesap.setHesapSahibi(request.hesapSahibi().trim());
        if (request.durum() != null) {
            hesap.setDurum(request.durum());
        }
        return HesapResponse.from(hesap);
    }

    /** Hiç kullanılmamış hesap silinir; rezervasyon, öğrenci, envanter ya da abonelik ödemesinde kullanılmışsa pasife alınır. */
    @Transactional
    public void delete(Long id) {
        Hesap hesap = getEntity(id);
        if (rezervasyonOdemeRepository.existsByHesapId(id) || ogrenciOdemeRepository.existsByHesapId(id)
                || envanterSatisOdemeRepository.existsByHesapId(id) || abonelikOdemeRepository.existsByHesapId(id)
                || iadeRepository.existsByHesapId(id)) {
            hesap.setDurum(HesapDurum.PASIF);
            return;
        }
        hesapRepository.delete(hesap);
    }

    /**
     * Ödeme yöntemine göre kayda bağlanacak hesabı çözer. Havale / EFT'de hesap seçimi zorunludur;
     * diğer yöntemlerde hesap tutulmaz (null döner, gönderilen hesap yok sayılır). Hesap oturumdaki işletmeye ait olmalı; pasif hesap yeni seçilemez
     * ({@code mevcutHesapId}, kayıtta zaten seçili olan hesabı belirtir ve değişmeden korunabilir).
     */
    public Hesap coz(Long hesapId, OdemeYontemi yontem, Long mevcutHesapId) {
        if (yontem != OdemeYontemi.HAVALE_EFT) {
            return null;
        }
        if (hesapId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Havale / EFT için hesap seçimi zorunludur");
        }
        Hesap hesap = getEntity(hesapId);
        if (hesap.getDurum() == HesapDurum.PASIF && !hesapId.equals(mevcutHesapId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Pasif hesap seçilemez");
        }
        return hesap;
    }

    /**
     * Boşluk/tireyi atar, büyük harfe çevirir; TR IBAN biçimini ve mod-97 sağlamasını doğrular.
     * Biçim: TR + 2 kontrol hanesi + 5 banka kodu + 1 rezerv (hepsi rakam) + 16 haneli hesap no (rakam ya da harf olabilir).
     */
    static String ibanNormalle(String ham) {
        String iban = ham.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
        if (!iban.startsWith("TR")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IBAN TR ile başlamalıdır");
        }
        if (iban.length() != 26) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "IBAN 26 karakter olmalıdır (girilen: " + iban.length() + ")");
        }
        if (!iban.matches("TR\\d{8}[0-9A-Z]{16}")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IBAN yalnızca rakam ve harf içermelidir");
        }
        StringBuilder sayisal = new StringBuilder();
        for (char c : (iban.substring(4) + iban.substring(0, 4)).toCharArray()) {
            sayisal.append(Character.digit(c, 36));
        }
        if (!new BigInteger(sayisal.toString()).mod(BigInteger.valueOf(97)).equals(BigInteger.ONE)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IBAN geçersiz; lütfen kontrol edin");
        }
        return iban;
    }

    private Hesap getEntity(Long id) {
        return hesapRepository.findByIdAndIsletmeId(id, isletmeContext.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Hesap bulunamadı: " + id));
    }
}
