package com.msc.halisaha.service;

import com.msc.halisaha.common.TurkceSirala;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.common.security.IsletmeContext;
import com.msc.halisaha.dto.EnvanterSetRequest;
import com.msc.halisaha.dto.EnvanterSetResponse;
import com.msc.halisaha.entity.Envanter;
import com.msc.halisaha.entity.EnvanterBeden;
import com.msc.halisaha.entity.EnvanterSet;
import com.msc.halisaha.entity.EnvanterTur;
import com.msc.halisaha.repository.EnvanterRepository;
import com.msc.halisaha.repository.EnvanterSatisRepository;
import com.msc.halisaha.repository.EnvanterSetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Envanter setleri. Set bir bedene ve kendi tutarına sahiptir; o bedendeki seçilen bedenli envanterleri ve seçilen bedensiz ürünleri içerir.
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
        List<Envanter> envanterler = envanterleriBul(isletmeId, request);
        EnvanterSet set = EnvanterSet.builder()
                .isletme(isletmeContext.getir())
                .ad(ad)
                .beden(request.beden())
                .tutar(request.tutar().setScale(2, RoundingMode.HALF_UP))
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
        List<Envanter> envanterler = envanterleriBul(isletmeId, request);
        set.setAd(ad);
        set.setBeden(request.beden());
        set.setTutar(request.tutar().setScale(2, RoundingMode.HALF_UP));
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

    /**
     * Seçilen envanterler: en az bir kayıt, bu işletmeye ait, tekrarsız, her türden en fazla bir tane; bedenli olanlar
     * set bedeninde olmalı ve stoğu bulunmalı. Stoğu biten ürünler tek mesajda bildirilir ve set oluşturulmaz.
     */
    private List<Envanter> envanterleriBul(Long isletmeId, EnvanterSetRequest request) {
        String beden = request.beden();
        if (!EnvanterBeden.gecerli(beden)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Geçersiz beden: " + beden);
        }
        List<Long> idler = request.envanterIdleri();
        Set<Long> tekil = new LinkedHashSet<>(idler);
        if (tekil.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Sete en az bir envanter eklenmelidir");
        }
        if (tekil.size() != idler.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Aynı envanter sete birden fazla kez eklenemez");
        }
        List<Envanter> sonuc = new ArrayList<>();
        List<String> stokYok = new ArrayList<>();
        Set<EnvanterTur> turler = EnumSet.noneOf(EnvanterTur.class);
        for (Long envanterId : tekil) {
            Envanter envanter = envanterRepository.findByIdAndIsletmeId(envanterId, isletmeId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Envanter bulunamadı: " + envanterId));
            if (envanter.getTur().isBedenli() && !beden.equals(envanter.getBeden())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        envanter.getTur().getAd() + " (" + envanter.getBeden() + ") set bedeniyle (" + beden + ") uyuşmuyor");
            }
            if (!turler.add(envanter.getTur())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Sette her envanter türünden yalnızca bir kayıt olabilir: " + envanter.getTur().getAd());
            }
            if (envanter.getAdet() <= 0) {
                stokYok.add(envanter.getTur().getAd());
            }
            sonuc.add(envanter);
        }
        if (!stokYok.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Set oluşturulamaz. Stoğu biten: " + String.join(", ", stokYok));
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
