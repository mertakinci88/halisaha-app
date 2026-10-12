package com.msc.halisaha.service;

import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.common.security.IsletmeContext;
import com.msc.halisaha.dto.OgrenciRequest;
import com.msc.halisaha.common.AppZaman;
import com.msc.halisaha.dto.OgrenciResponse;
import com.msc.halisaha.dto.PageResponse;
import com.msc.halisaha.entity.Grup;
import com.msc.halisaha.entity.OgrenciOdemeDurumu;
import com.msc.halisaha.entity.Ogrenci;
import com.msc.halisaha.entity.OgrenciDurum;
import com.msc.halisaha.repository.EnvanterSatisRepository;
import com.msc.halisaha.repository.GrupRepository;
import com.msc.halisaha.repository.OgrenciOdemeRepository;
import com.msc.halisaha.repository.OgrenciRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OgrenciService {

    private final OgrenciRepository ogrenciRepository;
    private final GrupRepository grupRepository;
    private final IsletmeContext isletmeContext;
    private final EnvanterSatisRepository envanterSatisRepository;
    private final OgrenciOdemeRepository ogrenciOdemeRepository;

    /** odemeBekleyen: yalnızca içinde bulunulan ayda ödemesi girilmemiş (Ödeme bekliyor / Ödenmedi) öğrenciler. */
    public PageResponse<OgrenciResponse> list(Long grupId, String q, boolean odemeBekleyen, int sayfa, int boyut) {
        PageRequest pageRequest = PageRequest.of(sayfa, boyut, Sort.by(Sort.Direction.ASC, "adSoyad"));
        LocalDate bugun = AppZaman.bugun();
        Page<Ogrenci> sonuc = ogrenciRepository.ara(isletmeContext.id(), grupId, q, odemeBekleyen,
                bugun.getYear(), bugun.getMonthValue(), pageRequest);
        Map<Long, OgrenciOdemeDurumu> durumlar = buAyDurumlari(sonuc.getContent());
        return PageResponse.from(sonuc, o -> OgrenciResponse.from(o, durumlar.get(o.getId())));
    }

    public OgrenciResponse getById(Long id) {
        Ogrenci ogrenci = getEntity(id);
        return yanit(ogrenci);
    }

    @Transactional
    public OgrenciResponse create(OgrenciRequest request) {
        Ogrenci ogrenci = Ogrenci.builder()
                .isletme(isletmeContext.getir())
                .grup(resolveGrup(request.grupId()))
                .adSoyad(request.adSoyad())
                .durum(request.durum() != null ? request.durum() : OgrenciDurum.AKTIF)
                .build();
        applyRequest(ogrenci, request);
        return yanit(ogrenciRepository.save(ogrenci));
    }

    @Transactional
    public OgrenciResponse update(Long id, OgrenciRequest request) {
        Ogrenci ogrenci = getEntity(id);
        ogrenci.setGrup(resolveGrup(request.grupId()));
        applyRequest(ogrenci, request);
        if (request.durum() != null) {
            ogrenci.setDurum(request.durum());
        }
        return yanit(ogrenci);
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
        ogrenci.setNot(request.not());
    }

    private OgrenciResponse yanit(Ogrenci ogrenci) {
        return OgrenciResponse.from(ogrenci, buAyDurumlari(List.of(ogrenci)).get(ogrenci.getId()));
    }

    /** İçinde bulunulan ayın durumu: ödeme kaydı varsa ODENDI; yoksa ayın 15'ine kadar ODEME_BEKLIYOR, sonrasında ODENMEDI. */
    private Map<Long, OgrenciOdemeDurumu> buAyDurumlari(Collection<Ogrenci> ogrenciler) {
        if (ogrenciler.isEmpty()) {
            return Map.of();
        }
        LocalDate bugun = AppZaman.bugun();
        Set<Long> odeyenler = ogrenciOdemeRepository
                .findDonem(ogrenciler.stream().map(Ogrenci::getId).toList(), bugun.getYear(), bugun.getMonthValue()).stream()
                .map(k -> k.getOgrenci().getId())
                .collect(Collectors.toSet());
        Map<Long, OgrenciOdemeDurumu> sonuc = new HashMap<>();
        for (Ogrenci o : ogrenciler) {
            sonuc.put(o.getId(), OgrenciOdemeDurumu.hesapla(odeyenler.contains(o.getId()), bugun.getYear(), bugun.getMonthValue(), bugun));
        }
        return sonuc;
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
