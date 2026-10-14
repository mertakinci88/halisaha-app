package com.msc.halisaha.service;

import com.msc.halisaha.common.AppZaman;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.common.security.IsletmeContext;
import com.msc.halisaha.dto.OgrenciOdemeAyRequest;
import com.msc.halisaha.dto.OgrenciOdemeAyResponse;
import com.msc.halisaha.dto.OgrenciOdemeKaydetRequest;
import com.msc.halisaha.dto.OgrenciOdemeYilResponse;
import com.msc.halisaha.entity.OgrenciOdemeDurumu;
import com.msc.halisaha.entity.Hesap;
import com.msc.halisaha.entity.OdemeYontemi;
import com.msc.halisaha.entity.Ogrenci;
import com.msc.halisaha.entity.OgrenciOdeme;
import com.msc.halisaha.repository.IadeRepository;
import com.msc.halisaha.repository.OgrenciOdemeRepository;
import com.msc.halisaha.repository.OgrenciRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Öğrencinin aylık ödemeleri. Her ay için ne kadar ödendiği elle girilir (ödeme şekli, tarih ve not ile); sabit tutar ya da
 * üst sınır yoktur. Ödeme şekli kullanıcı seçimidir (tutar girilmişse zorunlu). Kayıt, tutar girilmişse ya da yalnızca not
 * yazılmışsa tutulur; ikisi de boşsa ayın kaydı silinir. Tutar (> 0) girilmiş ay "Ödendi" sayılır; girilmemiş ay her ayın 15'ine kadar "Ödeme bekliyor", sonrasında "Ödenmedi" olur
 * (durum kaydedilmez, okunurken bugünün tarihinden hesaplanır). Tam / kısmi ayrımı yapılmaz.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OgrenciOdemeService {

    private final OgrenciOdemeRepository odemeRepository;
    private final OgrenciRepository ogrenciRepository;
    private final HesapService hesapService;
    private final IadeRepository iadeRepository;
    private final IsletmeContext isletmeContext;
    private final MevcutKullanici mevcutKullanici;
    private final GrupKapsami grupKapsami;

    public OgrenciOdemeYilResponse getir(Long ogrenciId, int yil) {
        ogrenciGetir(ogrenciId);
        return yanit(ogrenciId, yil);
    }

    @Transactional
    public OgrenciOdemeYilResponse kaydet(Long ogrenciId, int yil, OgrenciOdemeKaydetRequest request) {
        yilKontrol(yil);
        Ogrenci ogrenci = ogrenciGetir(ogrenciId);
        Map<Integer, OgrenciOdeme> mevcut = odemeRepository.findByOgrenciIdAndYil(ogrenciId, yil).stream()
                .collect(Collectors.toMap(OgrenciOdeme::getAy, Function.identity()));

        Set<Integer> gorulen = new HashSet<>();
        for (OgrenciOdemeAyRequest ayIstek : request.aylar()) {
            if (!gorulen.add(ayIstek.ay())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Aynı ay birden fazla kez gönderildi: " + ayIstek.ay());
            }
            aySakla(ogrenci, yil, ayIstek, mevcut.get(ayIstek.ay()));
        }
        odemeRepository.flush();
        return yanit(ogrenciId, yil);
    }

    private void aySakla(Ogrenci ogrenci, int yil, OgrenciOdemeAyRequest istek, OgrenciOdeme kayit) {
        BigDecimal odenen = istek.odenenTutar() != null ? istek.odenenTutar().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        String not = istek.not() != null && !istek.not().isBlank() ? istek.not().trim() : null;
        // İadesi yapılmış ayın ödemesi, iade edilen tutarın altına düşürülemez ve silinemez
        if (kayit != null) {
            BigDecimal iade = iadeRepository.ogrenciOdemeToplami(kayit.getId());
            if (odenen.compareTo(iade) < 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Bu ay için " + iade + " iade kaydı var; ödeme iade tutarının altına düşürülemez (ay: " + istek.ay() + ")");
            }
        }
        if (odenen.signum() == 0 && not == null) {
            if (kayit != null) {
                odemeRepository.delete(kayit);
            }
            return;
        }

        OdemeYontemi sekil = null;
        Hesap hesap = null;
        LocalDate tarih = null;
        if (odenen.signum() > 0) {
            if (istek.odemeSekli() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Ödeme şekli seçilmelidir (ay: " + istek.ay() + ")");
            }
            sekil = istek.odemeSekli();
            Long mevcutHesapId = kayit != null && kayit.getHesap() != null ? kayit.getHesap().getId() : null;
            hesap = hesapService.coz(istek.hesapId(), sekil, mevcutHesapId);
            tarih = istek.odemeTarihi() != null ? istek.odemeTarihi() : AppZaman.bugun();
        }

        if (kayit == null) {
            kayit = OgrenciOdeme.builder().ogrenci(ogrenci).yil(yil).ay(istek.ay()).build();
        }
        kayit.setOdenenTutar(odenen);
        kayit.setOdemeSekli(sekil);
        kayit.setHesap(hesap);
        kayit.setOdemeTarihi(tarih);
        kayit.setNot(not);
        kayit.setKullanici(mevcutKullanici.getir());
        odemeRepository.save(kayit);
    }

    private OgrenciOdemeYilResponse yanit(Long ogrenciId, int yil) {
        yilKontrol(yil);
        Map<Integer, OgrenciOdeme> kayitlar = odemeRepository.findByOgrenciIdAndYil(ogrenciId, yil).stream()
                .collect(Collectors.toMap(OgrenciOdeme::getAy, Function.identity()));

        LocalDate bugun = AppZaman.bugun();
        List<OgrenciOdemeAyResponse> aylar = new ArrayList<>();
        BigDecimal toplam = BigDecimal.ZERO;
        for (int ay = 1; ay <= 12; ay++) {
            OgrenciOdeme k = kayitlar.get(ay);
            if (k == null) {
                aylar.add(new OgrenciOdemeAyResponse(null, yil, ay, BigDecimal.ZERO, OgrenciOdemeDurumu.hesapla(false, yil, ay, bugun), null, null, null, null, null));
            } else {
                toplam = toplam.add(k.getOdenenTutar());
                aylar.add(new OgrenciOdemeAyResponse(k.getId(), yil, ay, k.getOdenenTutar(),
                        OgrenciOdemeDurumu.hesapla(k.getOdenenTutar().signum() > 0, yil, ay, bugun), k.getOdemeSekli(),
                        k.getHesap() != null ? k.getHesap().getId() : null,
                        k.getHesap() != null ? k.getHesap().getAd() : null, k.getOdemeTarihi(), k.getNot()));
            }
        }
        Set<Integer> yillar = new TreeSet<>(odemeRepository.yillar(ogrenciId));
        yillar.add(AppZaman.bugun().getYear());
        yillar.add(yil);
        return new OgrenciOdemeYilResponse(yil, bugun, aylar, toplam, List.copyOf(yillar));
    }

    private static void yilKontrol(int yil) {
        if (yil < 2000 || yil > 2100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Yıl 2000-2100 arasında olmalıdır");
        }
    }

    private Ogrenci ogrenciGetir(Long id) {
        Ogrenci ogrenci = ogrenciRepository.findByIdAndIsletmeId(id, isletmeContext.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Öğrenci bulunamadı: " + id));
        grupKapsami.kontrol(ogrenci);
        return ogrenci;
    }
}
