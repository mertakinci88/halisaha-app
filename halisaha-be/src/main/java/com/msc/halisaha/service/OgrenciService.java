package com.msc.halisaha.service;

import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.dto.OgrenciRequest;
import com.msc.halisaha.dto.OgrenciResponse;
import com.msc.halisaha.entity.Grup;
import com.msc.halisaha.entity.OdemeDurumu;
import com.msc.halisaha.entity.Ogrenci;
import com.msc.halisaha.entity.OgrenciDurum;
import com.msc.halisaha.repository.GrupRepository;
import com.msc.halisaha.repository.OgrenciRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OgrenciService {

    private final OgrenciRepository ogrenciRepository;
    private final GrupRepository grupRepository;

    public List<OgrenciResponse> list(Long grupId) {
        List<Ogrenci> ogrenciler = grupId != null
                ? ogrenciRepository.findByGrupId(grupId)
                : ogrenciRepository.findAll();
        return ogrenciler.stream().map(OgrenciResponse::from).toList();
    }

    public OgrenciResponse getById(Long id) {
        return OgrenciResponse.from(getEntity(id));
    }

    @Transactional
    public OgrenciResponse create(OgrenciRequest request) {
        Ogrenci ogrenci = Ogrenci.builder()
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
        if (!ogrenciRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Öğrenci bulunamadı: " + id);
        }
        ogrenciRepository.deleteById(id);
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
        ogrenci.setNot(request.not());
    }

    private Grup resolveGrup(Long grupId) {
        if (grupId == null) {
            return null;
        }
        return grupRepository.findById(grupId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Grup bulunamadı: " + grupId));
    }

    private Ogrenci getEntity(Long id) {
        return ogrenciRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Öğrenci bulunamadı: " + id));
    }
}
