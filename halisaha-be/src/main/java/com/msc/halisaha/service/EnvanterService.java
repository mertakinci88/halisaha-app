package com.msc.halisaha.service;

import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.common.security.IsletmeContext;
import com.msc.halisaha.dto.EnvanterRequest;
import com.msc.halisaha.dto.EnvanterResponse;
import com.msc.halisaha.entity.Envanter;
import com.msc.halisaha.entity.EnvanterBeden;
import com.msc.halisaha.entity.EnvanterTur;
import com.msc.halisaha.repository.EnvanterRepository;
import com.msc.halisaha.repository.EnvanterSatisKalemRepository;
import com.msc.halisaha.repository.EnvanterSetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnvanterService {

    private final EnvanterRepository envanterRepository;
    private final EnvanterSatisKalemRepository kalemRepository;
    private final EnvanterSetRepository setRepository;
    private final EnvanterStokService stokService;
    private final IsletmeContext isletmeContext;

    /** Envanter türüne (Türkçe alfabetik), sonra beden sırasına göre sabit sıralı liste. */
    public List<EnvanterResponse> list() {
        return envanterRepository.findByIsletmeId(isletmeContext.id()).stream()
                .sorted(Comparator.comparingInt((Envanter e) -> e.getTur().ordinal())
                        .thenComparingInt(e -> EnvanterBeden.sira(e.getBeden()))
                        .thenComparing(Envanter::getId))
                .map(EnvanterResponse::from)
                .toList();
    }

    public EnvanterResponse getById(Long id) {
        return EnvanterResponse.from(getEntity(id));
    }

    @Transactional
    public EnvanterResponse create(EnvanterRequest request) {
        Long isletmeId = isletmeContext.id();
        String beden = bedenBelirle(request.tur(), request.beden());
        if (envanterRepository.existsByIsletmeIdAndTurAndBeden(isletmeId, request.tur(), beden)) {
            throw cakisma(request.tur(), beden);
        }
        Envanter envanter = envanterRepository.save(Envanter.builder()
                .isletme(isletmeContext.getir())
                .tur(request.tur())
                .beden(beden)
                .adet(request.adet())
                .stokUyariAdedi(request.stokUyariAdedi() != null ? request.stokUyariAdedi() : 10)
                .satisFiyati(request.satisFiyati())
                .build());
        stokService.duzelt(envanter, 0, request.adet(), "ilk stok girişi");
        return EnvanterResponse.from(envanter);
    }

    @Transactional
    public EnvanterResponse update(Long id, EnvanterRequest request) {
        Envanter envanter = getEntity(id);
        String beden = bedenBelirle(request.tur(), request.beden());

        boolean kimlikDegisti = envanter.getTur() != request.tur() || !envanter.getBeden().equals(beden);
        if (kimlikDegisti) {
            if (kalemRepository.existsByEnvanterId(id)) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "Satışta kullanılan envanterin türü veya bedeni değiştirilemez; adet ve fiyat güncellenebilir");
            }
            if (setRepository.envanterKullaniliyor(id)) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "Sette kullanılan envanterin türü veya bedeni değiştirilemez; önce setten çıkarın");
            }
            if (envanterRepository.existsByIsletmeIdAndTurAndBedenAndIdNot(isletmeContext.id(), request.tur(), beden, id)) {
                throw cakisma(request.tur(), beden);
            }
        }
        int eskiAdet = envanter.getAdet();
        envanter.setTur(request.tur());
        envanter.setBeden(beden);
        envanter.setAdet(request.adet());
        envanter.setSatisFiyati(request.satisFiyati());
        if (request.stokUyariAdedi() != null) {
            envanter.setStokUyariAdedi(request.stokUyariAdedi());
        }
        stokService.duzelt(envanter, eskiAdet, request.adet(), "elle düzeltme");
        return EnvanterResponse.from(envanter);
    }

    @Transactional
    public void delete(Long id) {
        Envanter envanter = getEntity(id);
        if (kalemRepository.existsByEnvanterId(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Satışta kullanılan envanter silinemez");
        }
        if (setRepository.envanterKullaniliyor(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Sette kullanılan envanter silinemez; önce setten çıkarın");
        }
        envanterRepository.delete(envanter);
    }

    /** Bedenli türlerde geçerli bir beden zorunludur; diğerlerinde beden STANDART'tır. */
    private String bedenBelirle(EnvanterTur tur, String beden) {
        if (!tur.isBedenli()) {
            return EnvanterBeden.STANDART;
        }
        if (!EnvanterBeden.gecerli(beden)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    tur.getAd() + " için beden seçilmelidir (" + String.join(", ", EnvanterBeden.BEDENLER) + ")");
        }
        return beden;
    }

    private ApiException cakisma(EnvanterTur tur, String beden) {
        return new ApiException(HttpStatus.CONFLICT,
                tur.getAd() + " · " + beden + " zaten kayıtlı; mevcut kaydı güncelleyin ya da farklı bir beden seçin");
    }

    private Envanter getEntity(Long id) {
        return envanterRepository.findByIdAndIsletmeId(id, isletmeContext.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Envanter bulunamadı: " + id));
    }
}
