package com.msc.halisaha.service;

import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.common.security.IsletmeContext;
import com.msc.halisaha.dto.OgrenciRequest;
import com.msc.halisaha.dto.OgrenciResponse;
import com.msc.halisaha.dto.PageResponse;
import com.msc.halisaha.entity.Grup;
import com.msc.halisaha.entity.OdemeDurumu;
import com.msc.halisaha.entity.Ogrenci;
import com.msc.halisaha.entity.OgrenciDurum;
import com.msc.halisaha.repository.EnvanterSatisRepository;
import com.msc.halisaha.repository.GrupRepository;
import com.msc.halisaha.repository.OgrenciRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OgrenciService {

    private final OgrenciRepository ogrenciRepository;
    private final GrupRepository grupRepository;
    private final IsletmeContext isletmeContext;
    private final EnvanterSatisRepository envanterSatisRepository;
    private final HesapService hesapService;

    /** aidatBekleyen: yalnızca ödemesi tamamlanmamış (Ödenmedi / Kısmi ödendi) öğrenciler. */
    public PageResponse<OgrenciResponse> list(Long grupId, String q, boolean aidatBekleyen, int sayfa, int boyut) {
        PageRequest pageRequest = PageRequest.of(sayfa, boyut, Sort.by(Sort.Direction.ASC, "adSoyad"));
        Page<Ogrenci> sonuc = ogrenciRepository.ara(isletmeContext.id(), grupId, q, aidatBekleyen, pageRequest);
        return PageResponse.from(sonuc, OgrenciResponse::from);
    }

    public OgrenciResponse getById(Long id) {
        return OgrenciResponse.from(getEntity(id));
    }

    @Transactional
    public OgrenciResponse create(OgrenciRequest request) {
        Ogrenci ogrenci = Ogrenci.builder()
                .isletme(isletmeContext.getir())
                .grup(resolveGrup(request.grupId()))
                .adSoyad(request.adSoyad())
                .durum(request.durum() != null ? request.durum() : OgrenciDurum.AKTIF)
                .odemeDurumu(request.odemeDurumu() != null ? request.odemeDurumu() : OdemeDurumu.ODENMEDI)
                .build();
        applyRequest(ogrenci, request);
        return OgrenciResponse.from(ogrenciRepository.save(ogrenci));
    }

    @Transactional
    public OgrenciResponse update(Long id, OgrenciRequest request) {
        Ogrenci ogrenci = getEntity(id);
        ogrenci.setGrup(resolveGrup(request.grupId()));
        applyRequest(ogrenci, request);
        if (request.durum() != null) {
            ogrenci.setDurum(request.durum());
        }
        if (request.odemeDurumu() != null) {
            ogrenci.setOdemeDurumu(request.odemeDurumu());
        }
        return OgrenciResponse.from(ogrenci);
    }

    @Transactional
    public void delete(Long id) {
        Ogrenci ogrenci = getEntity(id);
        if (envanterSatisRepository.existsByOgrenciId(id)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Envanter satışı olan öğrenci silinemez; önce envanter satışlarını silin (teslim edilenlerin stoğu geri döner)");
        }
        ogrenciRepository.delete(ogrenci);
    }

    private void applyRequest(Ogrenci ogrenci, OgrenciRequest request) {
        ogrenci.setAdSoyad(request.adSoyad());
        ogrenci.setDogumTarih(request.dogumTarih());
        ogrenci.setVeliAdSoyad(request.veliAdSoyad());
        ogrenci.setVeliTelNo(request.veliTelNo());
        ogrenci.setAntrenmanGunSayisi(request.antrenmanGunSayisi());
        ogrenci.setAylikAidat(request.aylikAidat());
        ogrenci.setOdemePlani(request.odemePlani());
        ogrenci.setOdenenDonem(request.odenenDonem());
        ogrenci.setOdemeTarihi(request.odemeTarihi());
        ogrenci.setSonrakiOdemeTarih(request.sonrakiOdemeTarih());
        ogrenci.setOdenenTutar(request.odenenTutar());
        ogrenci.setOdemeSekli(request.odemeSekli());
        Long mevcutHesapId = ogrenci.getHesap() != null ? ogrenci.getHesap().getId() : null;
        ogrenci.setHesap(hesapService.coz(request.hesapId(), request.odemeSekli(), mevcutHesapId));
        ogrenci.setNot(request.not());
    }

    private Grup resolveGrup(Long grupId) {
        if (grupId == null) {
            return null;
        }
        return grupRepository.findByIdAndIsletmeId(grupId, isletmeContext.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Grup bulunamadı: " + grupId));
    }

    private Ogrenci getEntity(Long id) {
        return ogrenciRepository.findByIdAndIsletmeId(id, isletmeContext.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Öğrenci bulunamadı: " + id));
    }
}
