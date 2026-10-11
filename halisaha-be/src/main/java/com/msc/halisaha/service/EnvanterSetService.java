package com.msc.halisaha.service;

import com.msc.halisaha.common.TurkceSirala;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.common.security.IsletmeContext;
import com.msc.halisaha.dto.EnvanterSetRequest;
import com.msc.halisaha.dto.EnvanterSetResponse;
import com.msc.halisaha.entity.Envanter;
import com.msc.halisaha.entity.EnvanterSet;
import com.msc.halisaha.entity.EnvanterTur;
import com.msc.halisaha.repository.EnvanterRepository;
import com.msc.halisaha.repository.EnvanterSatisRepository;
import com.msc.halisaha.repository.EnvanterSetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Envanter setleri. Set, işletmenin envanter kayıtlarından (tür + beden) oluşur; her türden en fazla bir kayıt içerir.
 * Set sonradan değişse ya da silinse de geçmiş satışlar etkilenmez (satış kalemleri ve set adı satışta kopyalanır).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnvanterSetService {

    private final EnvanterSetRepository setRepository;
    private final EnvanterRepository envanterRepository;
    private final EnvanterSatisRepository satisRepository;
    private final IsletmeContext isletmeContext;
    private final MevcutKullanici mevcutKullanici;

    public List<EnvanterSetResponse> list() {
        return setRepository.listele(isletmeContext.id()).stream()
                .sorted(TurkceSirala.alfabetik(EnvanterSet::getAd))
                .map(EnvanterSetResponse::from)
                .toList();
    }

    public EnvanterSetResponse getById(Long id) {
        return EnvanterSetResponse.from(getEntity(id));
    }

    @Transactional
    public EnvanterSetResponse create(EnvanterSetRequest request) {
        Long isletmeId = isletmeContext.id();
        String ad = request.ad().trim();
        if (setRepository.existsByIsletmeIdAndAdIgnoreCase(isletmeId, ad)) {
            throw adCakismasi(ad);
        }
        List<Envanter> envanterler = envanterleriBul(isletmeId, request.envanterIdleri());
        EnvanterSet set = EnvanterSet.builder()
                .isletme(isletmeContext.getir())
                .ad(ad)
                .kullanici(mevcutKullanici.getir())
                .envanterler(envanterler)
                .build();
        return EnvanterSetResponse.from(setRepository.save(set));
    }

    @Transactional
    public EnvanterSetResponse update(Long id, EnvanterSetRequest request) {
        Long isletmeId = isletmeContext.id();
        EnvanterSet set = getEntity(id);
        String ad = request.ad().trim();
        if (setRepository.existsByIsletmeIdAndAdIgnoreCaseAndIdNot(isletmeId, ad, id)) {
            throw adCakismasi(ad);
        }
        List<Envanter> envanterler = envanterleriBul(isletmeId, request.envanterIdleri());
        set.setAd(ad);
        set.getEnvanterler().clear();
        set.getEnvanterler().addAll(envanterler);
        set.setKullanici(mevcutKullanici.getir());
        return EnvanterSetResponse.from(set);
    }

    @Transactional
    public void delete(Long id) {
        EnvanterSet set = getEntity(id);
        satisRepository.setBaglantisiniKaldir(id);
        setRepository.delete(set);
    }

    /** En az bir kayıt; hepsi bu işletmeye ait, tekrarsız ve her türden en fazla bir tane. */
    private List<Envanter> envanterleriBul(Long isletmeId, List<Long> idler) {
        Set<Long> tekil = new LinkedHashSet<>(idler);
        if (tekil.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Sete en az bir envanter eklenmelidir");
        }
        if (tekil.size() != idler.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Aynı envanter sete birden fazla kez eklenemez");
        }
        List<Envanter> sonuc = new ArrayList<>();
        Set<EnvanterTur> turler = EnumSet.noneOf(EnvanterTur.class);
        for (Long envanterId : tekil) {
            Envanter envanter = envanterRepository.findByIdAndIsletmeId(envanterId, isletmeId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Envanter bulunamadı: " + envanterId));
            if (!turler.add(envanter.getTur())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Sette her envanter türünden yalnızca bir kayıt olabilir: " + envanter.getTur().getAd());
            }
            sonuc.add(envanter);
        }
        return sonuc;
    }

    private ApiException adCakismasi(String ad) {
        return new ApiException(HttpStatus.CONFLICT, "\"" + ad + "\" adlı bir set zaten var");
    }

    private EnvanterSet getEntity(Long id) {
        return setRepository.bul(id, isletmeContext.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Set bulunamadı: " + id));
    }
}
