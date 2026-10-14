package com.msc.halisaha.service;

import com.msc.halisaha.common.AppZaman;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.common.security.IsletmeContext;
import com.msc.halisaha.dto.IadeRequest;
import com.msc.halisaha.dto.IadeResponse;
import com.msc.halisaha.entity.Abonelik;
import com.msc.halisaha.entity.AbonelikOdeme;
import com.msc.halisaha.entity.EnvanterSatis;
import com.msc.halisaha.entity.Iade;
import com.msc.halisaha.entity.OgrenciOdeme;
import com.msc.halisaha.entity.Rezervasyon;
import com.msc.halisaha.entity.RezervasyonOdeme;
import com.msc.halisaha.repository.AbonelikRepository;
import com.msc.halisaha.repository.EnvanterSatisRepository;
import com.msc.halisaha.repository.IadeRepository;
import com.msc.halisaha.repository.OgrenciOdemeRepository;
import com.msc.halisaha.repository.OgrenciRepository;
import com.msc.halisaha.repository.RezervasyonOdemeRepository;
import com.msc.halisaha.repository.RezervasyonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

/**
 * İade: alınan bir ödemenin geri verilmesi. Ödeme kayıtlarına dokunulmaz (ödeme durumu da değişmez); iade ayrı kayıt olarak tutulur
 * ve raporlarda iade tarihinde düşülür. Bir kayıt için toplam iade, o kayıtta alınan ödemeyi aşamaz.
 * Yanlış girilen ödemeyi silmekle karıştırılmamalıdır: iade, paranın gerçekten geri verildiğini gösterir.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IadeService {

    private final IadeRepository iadeRepository;
    private final RezervasyonRepository rezervasyonRepository;
    private final RezervasyonOdemeRepository rezervasyonOdemeRepository;
    private final OgrenciOdemeRepository ogrenciOdemeRepository;
    private final EnvanterSatisRepository envanterSatisRepository;
    private final AbonelikRepository abonelikRepository;
    private final OgrenciRepository ogrenciRepository;
    private final HesapService hesapService;
    private final IsletmeContext isletmeContext;
    private final MevcutKullanici mevcutKullanici;

    /** Tam olarak bir kaynak id'si verilmelidir. */
    public List<IadeResponse> liste(Long rezervasyonId, Long ogrenciOdemeId, Long envanterSatisId, Long abonelikId) {
        Long isletmeId = isletmeContext.id();
        int adet = (int) Stream.of(rezervasyonId, ogrenciOdemeId, envanterSatisId, abonelikId).filter(x -> x != null).count();
        if (adet != 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "İadeler tek bir kayıt için sorgulanmalıdır");
        }
        List<Iade> iadeler;
        if (rezervasyonId != null) {
            rezervasyonBul(rezervasyonId, isletmeId);
            iadeler = iadeRepository.rezervasyonIadeleri(rezervasyonId);
        } else if (ogrenciOdemeId != null) {
            ogrenciOdemeBul(ogrenciOdemeId, isletmeId);
            iadeler = iadeRepository.ogrenciOdemeIadeleri(ogrenciOdemeId);
        } else if (envanterSatisId != null) {
            envanterSatisBul(envanterSatisId, isletmeId);
            iadeler = iadeRepository.envanterSatisIadeleri(envanterSatisId);
        } else {
            abonelikBul(abonelikId, isletmeId);
            iadeler = iadeRepository.abonelikIadeleri(abonelikId);
        }
        return iadeler.stream().map(IadeResponse::from).toList();
    }

    /** Bir öğrencinin seçili yıldaki aylık ödemelerine yapılmış tüm iadeler (kaynakId, aylık ödeme kaydının id'sidir). */
    public List<IadeResponse> ogrenciYilListe(Long ogrenciId, int yil) {
        Long isletmeId = isletmeContext.id();
        ogrenciRepository.findByIdAndIsletmeId(ogrenciId, isletmeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Öğrenci bulunamadı: " + ogrenciId));
        return iadeRepository.ogrenciYilIadeleri(ogrenciId, yil, isletmeId).stream().map(IadeResponse::from).toList();
    }

    @Transactional
    public IadeResponse yap(IadeRequest request) {
        Long isletmeId = isletmeContext.id();
        int adet = (int) Stream.of(request.rezervasyonId(), request.ogrenciOdemeId(), request.envanterSatisId(), request.abonelikId())
                .filter(x -> x != null).count();
        if (adet != 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "İade tek bir kayda bağlanmalıdır");
        }
        BigDecimal tutar = request.tutar().setScale(2, RoundingMode.HALF_UP);
        LocalDate bugun = AppZaman.bugun();
        if (request.iadeTarihi() != null && request.iadeTarihi().isAfter(bugun)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "İade tarihi ileri bir tarih olamaz");
        }

        Iade.IadeBuilder iade = Iade.builder()
                .isletme(isletmeContext.getir())
                .tutar(tutar)
                .iadeSekli(request.iadeSekli())
                .hesap(hesapService.coz(request.hesapId(), request.iadeSekli(), null))
                .iadeTarihi(request.iadeTarihi() != null ? request.iadeTarihi() : bugun)
                .neden(request.neden() != null && !request.neden().isBlank() ? request.neden().trim() : null)
                .kullanici(mevcutKullanici.getir())
                .kayitTarih(AppZaman.simdi());

        BigDecimal odenen;
        BigDecimal oncekiIade;
        if (request.rezervasyonId() != null) {
            Rezervasyon r = rezervasyonBul(request.rezervasyonId(), isletmeId);
            odenen = rezervasyonOdemeRepository.findByRezervasyonId(r.getId()).stream()
                    .map(RezervasyonOdeme::getTutar).reduce(BigDecimal.ZERO, BigDecimal::add);
            oncekiIade = iadeRepository.rezervasyonToplami(r.getId());
            iade.rezervasyon(r);
        } else if (request.ogrenciOdemeId() != null) {
            OgrenciOdeme o = ogrenciOdemeBul(request.ogrenciOdemeId(), isletmeId);
            odenen = o.getOdenenTutar();
            oncekiIade = iadeRepository.ogrenciOdemeToplami(o.getId());
            iade.ogrenciOdeme(o);
        } else if (request.envanterSatisId() != null) {
            EnvanterSatis s = envanterSatisBul(request.envanterSatisId(), isletmeId);
            odenen = s.getOdenenTutar();
            oncekiIade = iadeRepository.envanterSatisToplami(s.getId());
            iade.envanterSatis(s);
        } else {
            Abonelik a = abonelikBul(request.abonelikId(), isletmeId);
            odenen = a.getOdemeler().stream().map(AbonelikOdeme::getTutar).reduce(BigDecimal.ZERO, BigDecimal::add);
            oncekiIade = iadeRepository.abonelikToplami(a.getId());
            iade.abonelik(a);
        }

        BigDecimal kalan = odenen.subtract(oncekiIade);
        if (tutar.compareTo(kalan) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "İade tutarı iade edilebilecek tutarı (" + kalan.max(BigDecimal.ZERO) + ") aşamaz");
        }
        return IadeResponse.from(iadeRepository.save(iade.build()));
    }

    /** Hatalı girilen iade kaydını siler (para gerçekte iade edilmediyse). */
    @Transactional
    public void sil(Long id) {
        Iade iade = iadeRepository.bul(id, isletmeContext.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "İade bulunamadı: " + id));
        iadeRepository.delete(iade);
    }

    private Rezervasyon rezervasyonBul(Long id, Long isletmeId) {
        return rezervasyonRepository.findByIdAndIsletmeId(id, isletmeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Rezervasyon bulunamadı: " + id));
    }

    private OgrenciOdeme ogrenciOdemeBul(Long id, Long isletmeId) {
        return ogrenciOdemeRepository.findById(id)
                .filter(o -> o.getOgrenci().getIsletme().getId().equals(isletmeId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Öğrenci ödemesi bulunamadı: " + id));
    }

    private EnvanterSatis envanterSatisBul(Long id, Long isletmeId) {
        return envanterSatisRepository.findById(id)
                .filter(s -> s.getIsletme().getId().equals(isletmeId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Envanter satışı bulunamadı: " + id));
    }

    private Abonelik abonelikBul(Long id, Long isletmeId) {
        return abonelikRepository.bul(id, isletmeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Abonelik bulunamadı: " + id));
    }
}
