package com.msc.halisaha.service;

import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.dto.RezervasyonRequest;
import com.msc.halisaha.dto.RezervasyonResponse;
import com.msc.halisaha.dto.RezervasyonUrunRequest;
import com.msc.halisaha.dto.RezervasyonUrunResponse;
import com.msc.halisaha.entity.OdemeDurumu;
import com.msc.halisaha.entity.Rezervasyon;
import com.msc.halisaha.entity.RezervasyonDurum;
import com.msc.halisaha.entity.RezervasyonUrun;
import com.msc.halisaha.entity.Saha;
import com.msc.halisaha.entity.Urun;
import com.msc.halisaha.repository.RezervasyonRepository;
import com.msc.halisaha.repository.RezervasyonUrunRepository;
import com.msc.halisaha.repository.SahaRepository;
import com.msc.halisaha.repository.UrunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RezervasyonService {

    private final RezervasyonRepository rezervasyonRepository;
    private final SahaRepository sahaRepository;
    private final RezervasyonUrunRepository rezervasyonUrunRepository;
    private final UrunRepository urunRepository;

    public List<RezervasyonResponse> list(Long sahaId, LocalDate tarih) {
        List<Rezervasyon> rezervasyonlar;
        if (sahaId != null && tarih != null) {
            rezervasyonlar = rezervasyonRepository.findBySahaIdAndBaslangicTarihBetween(
                    sahaId, tarih.atStartOfDay(), tarih.plusDays(1).atStartOfDay());
        } else if (sahaId != null) {
            rezervasyonlar = rezervasyonRepository.findBySahaId(sahaId);
        } else {
            rezervasyonlar = rezervasyonRepository.findAll();
        }
        return rezervasyonlar.stream().map(this::toResponse).toList();
    }

    public RezervasyonResponse getById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public RezervasyonResponse create(RezervasyonRequest request) {
        validateTarihAraligi(request.baslangicTarih(), request.bitisTarih());
        Saha saha = getSaha(request.sahaId());
        checkOverlap(request.sahaId(), request.baslangicTarih(), request.bitisTarih(), null);

        Rezervasyon rezervasyon = Rezervasyon.builder()
                .saha(saha)
                .adSoyad(request.adSoyad())
                .baslangicTarih(request.baslangicTarih())
                .bitisTarih(request.bitisTarih())
                .telNo(request.telNo())
                .odemeYontemi(request.odemeYontemi())
                .odemeDurumu(request.odemeDurumu() != null ? request.odemeDurumu() : OdemeDurumu.ODENMEDI)
                .odenecekTutar(hesaplaSahaUcreti(saha, request.baslangicTarih(), request.bitisTarih()))
                .build();

        return toResponse(rezervasyonRepository.save(rezervasyon));
    }

    @Transactional
    public RezervasyonResponse update(Long id, RezervasyonRequest request) {
        Rezervasyon rezervasyon = getEntity(id);
        validateTarihAraligi(request.baslangicTarih(), request.bitisTarih());
        Saha saha = getSaha(request.sahaId());
        checkOverlap(request.sahaId(), request.baslangicTarih(), request.bitisTarih(), id);

        rezervasyon.setSaha(saha);
        rezervasyon.setAdSoyad(request.adSoyad());
        rezervasyon.setBaslangicTarih(request.baslangicTarih());
        rezervasyon.setBitisTarih(request.bitisTarih());
        rezervasyon.setTelNo(request.telNo());
        rezervasyon.setOdemeYontemi(request.odemeYontemi());
        if (request.odemeDurumu() != null) {
            rezervasyon.setOdemeDurumu(request.odemeDurumu());
        }

        recalculateTotal(rezervasyon);
        return toResponse(rezervasyon);
    }

    @Transactional
    public RezervasyonResponse cancel(Long id) {
        Rezervasyon rezervasyon = getEntity(id);
        rezervasyon.setDurum(RezervasyonDurum.IPTAL);
        return toResponse(rezervasyon);
    }

    @Transactional
    public void delete(Long id) {
        if (!rezervasyonRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Rezervasyon bulunamadı: " + id);
        }
        rezervasyonRepository.deleteById(id);
    }

    @Transactional
    public RezervasyonResponse urunEkle(Long rezervasyonId, RezervasyonUrunRequest request) {
        Rezervasyon rezervasyon = getEntity(rezervasyonId);
        Urun urun = urunRepository.findById(request.urunId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ürün bulunamadı: " + request.urunId()));

        RezervasyonUrun rezervasyonUrun = RezervasyonUrun.builder()
                .rezervasyon(rezervasyon)
                .urun(urun)
                .miktar(request.miktar())
                .build();
        rezervasyonUrunRepository.save(rezervasyonUrun);

        recalculateTotal(rezervasyon);
        return toResponse(rezervasyon);
    }

    @Transactional
    public RezervasyonResponse urunCikar(Long rezervasyonId, Long rezervasyonUrunId) {
        Rezervasyon rezervasyon = getEntity(rezervasyonId);
        RezervasyonUrun rezervasyonUrun = rezervasyonUrunRepository.findById(rezervasyonUrunId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Rezervasyon ürünü bulunamadı: " + rezervasyonUrunId));

        if (!rezervasyonUrun.getRezervasyon().getId().equals(rezervasyonId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bu ürün bu rezervasyona ait değil");
        }

        rezervasyonUrunRepository.delete(rezervasyonUrun);
        recalculateTotal(rezervasyon);
        return toResponse(rezervasyon);
    }

    private void recalculateTotal(Rezervasyon rezervasyon) {
        BigDecimal sahaUcreti = hesaplaSahaUcreti(rezervasyon.getSaha(), rezervasyon.getBaslangicTarih(), rezervasyon.getBitisTarih());
        BigDecimal urunToplam = rezervasyonUrunRepository.findByRezervasyonId(rezervasyon.getId()).stream()
                .map(ru -> ru.getUrun().getFiyat().multiply(BigDecimal.valueOf(ru.getMiktar())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        rezervasyon.setOdenecekTutar(sahaUcreti.add(urunToplam).setScale(2, RoundingMode.HALF_UP));
    }

    private BigDecimal hesaplaSahaUcreti(Saha saha, LocalDateTime baslangic, LocalDateTime bitis) {
        long dakika = Duration.between(baslangic, bitis).toMinutes();
        BigDecimal saat = BigDecimal.valueOf(dakika).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
        return saha.getSaatlikUcret().multiply(saat).setScale(2, RoundingMode.HALF_UP);
    }

    private void validateTarihAraligi(LocalDateTime baslangic, LocalDateTime bitis) {
        if (!bitis.isAfter(baslangic)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bitiş tarihi başlangıç tarihinden sonra olmalıdır");
        }
    }

    private void checkOverlap(Long sahaId, LocalDateTime baslangic, LocalDateTime bitis, Long excludeId) {
        List<Rezervasyon> overlapping = rezervasyonRepository.findOverlapping(sahaId, baslangic, bitis, excludeId);
        if (!overlapping.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "Bu saha için seçilen saat aralığında aktif bir rezervasyon zaten var");
        }
    }

    private Saha getSaha(Long sahaId) {
        return sahaRepository.findById(sahaId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Saha bulunamadı: " + sahaId));
    }

    private Rezervasyon getEntity(Long id) {
        return rezervasyonRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Rezervasyon bulunamadı: " + id));
    }

    private RezervasyonResponse toResponse(Rezervasyon rezervasyon) {
        List<RezervasyonUrunResponse> urunler = rezervasyonUrunRepository.findByRezervasyonId(rezervasyon.getId()).stream()
                .map(ru -> new RezervasyonUrunResponse(
                        ru.getId(),
                        ru.getUrun().getId(),
                        ru.getUrun().getAd(),
                        ru.getMiktar(),
                        ru.getUrun().getFiyat(),
                        ru.getUrun().getFiyat().multiply(BigDecimal.valueOf(ru.getMiktar()))
                ))
                .toList();

        return new RezervasyonResponse(
                rezervasyon.getId(),
                rezervasyon.getSaha().getId(),
                rezervasyon.getSaha().getAd(),
                rezervasyon.getAdSoyad(),
                rezervasyon.getBaslangicTarih(),
                rezervasyon.getBitisTarih(),
                rezervasyon.getDurum(),
                rezervasyon.getTelNo(),
                rezervasyon.getOdemeYontemi(),
                rezervasyon.getOdemeDurumu(),
                rezervasyon.getOdenecekTutar(),
                urunler
        );
    }
}
