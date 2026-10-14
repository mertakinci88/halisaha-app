package com.msc.halisaha.service;

import com.msc.halisaha.common.TurkceSirala;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.common.security.IsletmeContext;
import com.msc.halisaha.dto.GrupRequest;
import com.msc.halisaha.dto.GrupResponse;
import com.msc.halisaha.entity.Grup;
import com.msc.halisaha.repository.GrupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GrupService {

    private final GrupRepository grupRepository;
    private final IsletmeContext isletmeContext;
    private final GrupKapsami grupKapsami;

    public List<GrupResponse> list() {
        return grupRepository.findByIsletmeId(isletmeContext.id()).stream()
                .filter(g -> grupKapsami.grupIzinli(g.getId()))
                .sorted(TurkceSirala.<Grup>alfabetik(Grup::getAd).thenComparing(Grup::getId))
                .map(GrupResponse::from)
                .toList();
    }

    /** Grup yetkisi kısıtına bakmadan işletmenin tüm grupları (yalnızca ad; transfer hedefi seçimi için). */
    public List<GrupResponse> hedefListe() {
        return grupRepository.findByIsletmeId(isletmeContext.id()).stream()
                .sorted(TurkceSirala.<Grup>alfabetik(Grup::getAd).thenComparing(Grup::getId))
                .map(GrupResponse::from)
                .toList();
    }

    public GrupResponse getById(Long id) {
        return GrupResponse.from(getEntity(id));
    }

    @Transactional
    public GrupResponse create(GrupRequest request) {
        grupDegisimiKontrol();
        Grup grup = Grup.builder().isletme(isletmeContext.getir()).ad(request.ad()).build();
        return GrupResponse.from(grupRepository.save(grup));
    }

    @Transactional
    public GrupResponse update(Long id, GrupRequest request) {
        Grup grup = getEntity(id);
        grup.setAd(request.ad());
        return GrupResponse.from(grup);
    }

    @Transactional
    public void delete(Long id) {
        grupDegisimiKontrol();
        grupRepository.delete(getEntity(id));
    }

    /** Grup kısıtlı personel grup ekleyip silemez: eklediği grubu göremez, sildiği başkalarının öğrencilerini etkiler. */
    private void grupDegisimiKontrol() {
        if (grupKapsami.kisitli()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Grup yetkiniz kısıtlı olduğu için grup ekleyemez veya silemezsiniz");
        }
    }

    private Grup getEntity(Long id) {
        grupKapsami.kontrolGrup(id);
        return grupRepository.findByIdAndIsletmeId(id, isletmeContext.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Grup bulunamadı: " + id));
    }
}
