package com.msc.halisaha.service;

import com.msc.halisaha.common.AppZaman;
import com.msc.halisaha.common.TurkceSirala;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.common.security.IsletmeContext;
import com.msc.halisaha.dto.YoklamaKaydetRequest;
import com.msc.halisaha.dto.YoklamaSatir;
import com.msc.halisaha.entity.Ogrenci;
import com.msc.halisaha.entity.OgrenciDurum;
import com.msc.halisaha.entity.Yoklama;
import com.msc.halisaha.repository.GrupRepository;
import com.msc.halisaha.repository.OgrenciRepository;
import com.msc.halisaha.repository.YoklamaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class YoklamaService {

    private final YoklamaRepository yoklamaRepository;
    private final OgrenciRepository ogrenciRepository;
    private final GrupRepository grupRepository;
    private final IsletmeContext isletmeContext;
    private final MevcutKullanici mevcutKullanici;
    private final GrupKapsami grupKapsami;

    /** Grubun aktif öğrencileri ve o günkü yoklamaları (öğrenci adına göre alfabetik). */
    public List<YoklamaSatir> listele(LocalDate tarih, Long grupId) {
        Long isletmeId = isletmeContext.id();
        List<Ogrenci> ogrenciler = aktifOgrenciler(isletmeId, grupId);
        Map<Long, Yoklama> mevcut = mevcutYoklamalar(isletmeId, tarih, ogrenciler);

        return ogrenciler.stream()
                .map(o -> {
                    Yoklama y = mevcut.get(o.getId());
                    return new YoklamaSatir(o.getId(), o.getAdSoyad(),
                            y != null ? y.getDurum() : null, y != null ? y.getAciklama() : null);
                })
                .toList();
    }

    /**
     * Seçili gün ve grubun yoklamasını tek seferde kaydeder. Durumu olmayan öğrencinin mevcut kaydı silinir
     * (işaretlenmemiş hâle döner); durumsuz not tutulmaz.
     */
    @Transactional
    public List<YoklamaSatir> kaydet(LocalDate tarih, Long grupId, YoklamaKaydetRequest request) {
        if (tarih.isAfter(AppZaman.bugun())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Gelecek bir tarih için yoklama alınamaz");
        }
        Long isletmeId = isletmeContext.id();
        List<Ogrenci> ogrenciler = aktifOgrenciler(isletmeId, grupId);
        Map<Long, Ogrenci> ogrenciById = ogrenciler.stream().collect(Collectors.toMap(Ogrenci::getId, Function.identity()));
        Map<Long, Yoklama> mevcut = mevcutYoklamalar(isletmeId, tarih, ogrenciler);

        // Önce tüm istek doğrulanır; hatalıysa hiçbir kayıt değişmez
        Set<Long> gorulen = new HashSet<>();
        for (YoklamaKaydetRequest.Kayit kayit : request.kayitlar()) {
            Ogrenci ogrenci = ogrenciById.get(kayit.ogrenciId());
            if (ogrenci == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Öğrenci bu grupta aktif değil: " + kayit.ogrenciId());
            }
            if (!gorulen.add(kayit.ogrenciId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Aynı öğrenci birden fazla kez gönderildi: " + ogrenci.getAdSoyad());
            }
            if (kayit.durum() == null && temizNot(kayit.not()) != null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Durum seçilmeyen öğrenci için not girilemez: " + ogrenci.getAdSoyad());
            }
        }

        for (YoklamaKaydetRequest.Kayit kayit : request.kayitlar()) {
            Ogrenci ogrenci = ogrenciById.get(kayit.ogrenciId());
            String not = temizNot(kayit.not());
            Yoklama y = mevcut.get(ogrenci.getId());

            if (kayit.durum() == null) {
                if (y != null) {
                    yoklamaRepository.delete(y);
                }
                continue;
            }
            if (y == null) {
                y = Yoklama.builder()
                        .isletme(isletmeContext.getir())
                        .ogrenci(ogrenci)
                        .tarih(tarih)
                        .build();
            }
            y.setDurum(kayit.durum());
            y.setAciklama(not);
            y.setKullanici(mevcutKullanici.getir());
            yoklamaRepository.save(y);
        }
        yoklamaRepository.flush();
        return listele(tarih, grupId);
    }

    private static String temizNot(String not) {
        return not == null || not.isBlank() ? null : not.trim();
    }

    private List<Ogrenci> aktifOgrenciler(Long isletmeId, Long grupId) {
        grupKapsami.kontrolGrup(grupId);
        grupRepository.findByIdAndIsletmeId(grupId, isletmeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Grup bulunamadı: " + grupId));
        return ogrenciRepository.findByIsletmeIdAndGrupIdAndDurum(isletmeId, grupId, OgrenciDurum.AKTIF).stream()
                .sorted(TurkceSirala.<Ogrenci>alfabetik(Ogrenci::getAdSoyad).thenComparing(Ogrenci::getId))
                .toList();
    }

    private Map<Long, Yoklama> mevcutYoklamalar(Long isletmeId, LocalDate tarih, List<Ogrenci> ogrenciler) {
        if (ogrenciler.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = ogrenciler.stream().map(Ogrenci::getId).toList();
        return yoklamaRepository.findByIsletmeIdAndTarihAndOgrenciIdIn(isletmeId, tarih, ids).stream()
                .collect(Collectors.toMap(y -> y.getOgrenci().getId(), Function.identity()));
    }
}
