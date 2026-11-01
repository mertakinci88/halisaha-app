package com.msc.halisaha.service;

import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.common.security.IsletmeContext;
import com.msc.halisaha.dto.EnvanterTurRequest;
import com.msc.halisaha.dto.EnvanterTurResponse;
import com.msc.halisaha.entity.EnvanterTur;
import com.msc.halisaha.repository.EnvanterRepository;
import com.msc.halisaha.repository.EnvanterTurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** İşletmenin envanter türleri ve türe özel bedenleri. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnvanterTurService {

    private static final Collator TR = Collator.getInstance(Locale.forLanguageTag("tr"));

    private final EnvanterTurRepository turRepository;
    private final EnvanterRepository envanterRepository;
    private final IsletmeContext isletmeContext;

    /** Türkçe alfabetik sıralı liste; kullanimda=true olan türün envanter kaydı vardır, silinemez. */
    public List<EnvanterTurResponse> list() {
        Long isletmeId = isletmeContext.id();
        Set<Long> kullanimda = new HashSet<>(turRepository.kullanimdakiIdler(isletmeId));
        return turRepository.findByIsletmeId(isletmeId).stream()
                .sorted(Comparator.comparing(EnvanterTur::getAd, TR))
                .map(t -> EnvanterTurResponse.from(t, kullanimda.contains(t.getId())))
                .toList();
    }

    @Transactional
    public EnvanterTurResponse create(EnvanterTurRequest request) {
        Long isletmeId = isletmeContext.id();
        String ad = request.ad().trim();
        if (turRepository.existsByIsletmeIdAndAdIgnoreCase(isletmeId, ad)) {
            throw adCakismasi(ad);
        }
        EnvanterTur tur = turRepository.save(EnvanterTur.builder()
                .isletme(isletmeContext.getir())
                .ad(ad)
                .bedenli(request.bedenli())
                .birim(request.birim())
                .bedenler(bedenleriBelirle(request))
                .build());
        return EnvanterTurResponse.from(tur, false);
    }

    @Transactional
    public EnvanterTurResponse update(Long id, EnvanterTurRequest request) {
        Long isletmeId = isletmeContext.id();
        EnvanterTur tur = getEntity(id);
        String ad = request.ad().trim();
        if (turRepository.existsByIsletmeIdAndAdIgnoreCaseAndIdNot(isletmeId, ad, id)) {
            throw adCakismasi(ad);
        }
        boolean kullanimda = envanterRepository.existsByTurId(id);
        if (kullanimda && tur.isBedenli() != request.bedenli()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    tur.getAd() + " için envanter kaydı var; bedenli / bedensiz seçimi değiştirilemez");
        }
        List<String> bedenler = bedenleriBelirle(request);
        for (String eski : tur.getBedenler()) {
            if (!bedenler.contains(eski) && envanterRepository.existsByTurIdAndBeden(id, eski)) {
                throw new ApiException(HttpStatus.CONFLICT,
                        eski + " bedeninde envanter kaydı var; önce o envanteri silin ya da başka bedene alın");
            }
        }
        tur.setAd(ad);
        tur.setBedenli(request.bedenli());
        tur.setBirim(request.birim());
        tur.getBedenler().clear();
        tur.getBedenler().addAll(bedenler);
        return EnvanterTurResponse.from(tur, kullanimda);
    }

    @Transactional
    public void delete(Long id) {
        EnvanterTur tur = getEntity(id);
        if (envanterRepository.existsByTurId(id)) {
            throw new ApiException(HttpStatus.CONFLICT, tur.getAd() + " için envanter kaydı var; önce envanter kayıtlarını silin");
        }
        turRepository.delete(tur);
    }

    /** Bedenli türde en az bir, tekrarsız ve en fazla 10 karakterlik beden; bedensiz türde boş liste. */
    private List<String> bedenleriBelirle(EnvanterTurRequest request) {
        List<String> sonuc = new ArrayList<>();
        if (!request.bedenli()) {
            return sonuc;
        }
        if (request.bedenler() != null) {
            for (String ham : request.bedenler()) {
                String beden = ham == null ? "" : ham.trim();
                if (beden.isEmpty()) {
                    continue;
                }
                if (beden.length() > 10) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Beden en fazla 10 karakter olabilir: " + beden);
                }
                if (beden.equalsIgnoreCase(EnvanterTur.STANDART)) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "STANDART beden olarak kullanılamaz");
                }
                if (sonuc.stream().anyMatch(beden::equalsIgnoreCase)) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Beden birden fazla kez girildi: " + beden);
                }
                sonuc.add(beden);
            }
        }
        if (sonuc.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bedenli envanter için en az bir beden girilmelidir");
        }
        return sonuc;
    }

    private ApiException adCakismasi(String ad) {
        return new ApiException(HttpStatus.CONFLICT, "\"" + ad + "\" adlı bir envanter zaten var");
    }

    private EnvanterTur getEntity(Long id) {
        return turRepository.findByIdAndIsletmeId(id, isletmeContext.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Envanter türü bulunamadı: " + id));
    }
}
