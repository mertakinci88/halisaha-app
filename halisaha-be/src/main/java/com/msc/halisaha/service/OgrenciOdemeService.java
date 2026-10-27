package com.msc.halisaha.service;

import com.msc.halisaha.common.AppZaman;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.common.security.IsletmeContext;
import com.msc.halisaha.dto.OgrenciOdemeAlinirRequest;
import com.msc.halisaha.dto.OgrenciOdemeAyResponse;
import com.msc.halisaha.dto.OgrenciOdemeRequest;
import com.msc.halisaha.dto.OgrenciOdemeResponse;
import com.msc.halisaha.dto.OgrenciOdemeYilResponse;
import com.msc.halisaha.entity.OgrenciOdemeDurumu;
import com.msc.halisaha.entity.Hesap;
import com.msc.halisaha.entity.OdemeYontemi;
import com.msc.halisaha.entity.Ogrenci;
import com.msc.halisaha.entity.OgrenciOdeme;
import com.msc.halisaha.repository.IadeRepository;
import com.msc.halisaha.repository.OgrenciOdemeRepository;
import com.msc.halisaha.repository.OgrenciRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Öğrencinin aylık ödemeleri. Bir ayın birden fazla ödemesi olabilir (kısmi ödeme); her ödemenin tutarı, yöntemi, tarihi ve notu
 * elle girilir, sabit tutar ya da üst sınır yoktur. Her ödemede "tamamı ödendi" ya da "kısmi" seçilir: ayda en az bir tamam ödeme
 * varsa ay Ödendi, yalnızca kısmi ödemeler varsa Kısmi ödendi (ayın 15'inden sonra Kısmi gecikmiş), hiç ödeme yoksa 15'ine kadar
 * Ödeme bekliyor, sonrasında Ödenmedi olur (durum kaydedilmez, okunurken hesaplanır). Tutarsız (0) ödeme, açıklamalı olarak ayı
 * parasız kapatır (tamam) ya da yalnızca not düşer (kısmi).
 * Kalan para başka bir ayda alınırsa ödeme borcu olan aya, gerçek ödeme tarihiyle girilir; kasa raporları ödeme tarihine göre çalışır.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OgrenciOdemeService {

    private final OgrenciOdemeRepository odemeRepository;
    private final OgrenciRepository ogrenciRepository;
    private final HesapService hesapService;
    private final IadeRepository iadeRepository;
    private final IsletmeContext isletmeContext;
    private final MevcutKullanici mevcutKullanici;
    private final GrupKapsami grupKapsami;

    public OgrenciOdemeYilResponse getir(Long ogrenciId, int yil) {
        ogrenciGetir(ogrenciId);
        return yanit(ogrenciId, yil);
    }

    /**
     * Öğrenciden ödeme alınıp alınmayacağı. Kapatılırken öğrencinin ödeme kaydı varsa kullanıcı uyarıyı onaylamadan işlem yapılmaz;
     * geçmiş kayıtlara dokunulmaz, yalnızca öğrenci ödeme takibinin dışına alınır.
     */
    @Transactional
    public void alinirDegistir(Long ogrenciId, OgrenciOdemeAlinirRequest istek) {
        Ogrenci ogrenci = ogrenciGetir(ogrenciId);
        if (!istek.alinir() && ogrenci.isOdemeAlinir() && !Boolean.TRUE.equals(istek.onay())
                && odemeRepository.existsByOgrenciId(ogrenciId)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Bu öğrencinin geçmiş ödeme kayıtları var; ödeme alımı kapatılırsa raporlarda ve borçlu listesinde yer almaz. Onay gerekir");
        }
        ogrenci.setOdemeAlinir(istek.alinir());
    }

    @Transactional
    public OgrenciOdemeYilResponse ekle(Long ogrenciId, int yil, int ay, OgrenciOdemeRequest istek) {
        yilKontrol(yil);
        ayKontrol(ay);
        Ogrenci ogrenci = ogrenciGetir(ogrenciId);
        if (!ogrenci.isOdemeAlinir()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bu öğrenciden ödeme alınmıyor; ödeme eklemek için önce öğrenci bilgilerinden ödeme alımını açın");
        }
        OgrenciOdeme kayit = OgrenciOdeme.builder().ogrenci(ogrenci).yil(yil).ay(ay).build();
        doldur(kayit, istek);
        odemeRepository.saveAndFlush(kayit);
        return yanit(ogrenciId, yil);
    }

    @Transactional
    public OgrenciOdemeYilResponse guncelle(Long ogrenciId, int yil, int ay, Long odemeId, OgrenciOdemeRequest istek) {
        ogrenciGetir(ogrenciId);
        OgrenciOdeme kayit = odemeBul(ogrenciId, yil, ay, odemeId);
        // İadesi yapılmış ödeme, iade edilen tutarın altına düşürülemez
        BigDecimal iade = iadeRepository.ogrenciOdemeToplami(kayit.getId());
        if (tutar(istek).compareTo(iade) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bu ödeme için " + iade + " iade kaydı var; tutar iade tutarının altına düşürülemez");
        }
        doldur(kayit, istek);
        odemeRepository.saveAndFlush(kayit);
        return yanit(ogrenciId, yil);
    }

    @Transactional
    public OgrenciOdemeYilResponse sil(Long ogrenciId, int yil, int ay, Long odemeId) {
        ogrenciGetir(ogrenciId);
        OgrenciOdeme kayit = odemeBul(ogrenciId, yil, ay, odemeId);
        if (iadeRepository.ogrenciOdemeToplami(kayit.getId()).signum() > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bu ödeme için iade kaydı var; ödeme silinemez");
        }
        odemeRepository.delete(kayit);
        odemeRepository.flush();
        return yanit(ogrenciId, yil);
    }

    private OgrenciOdeme odemeBul(Long ogrenciId, int yil, int ay, Long odemeId) {
        return odemeRepository.findByIdAndOgrenciId(odemeId, ogrenciId)
                .filter(k -> k.getYil() == yil && k.getAy() == ay)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ödeme bulunamadı: " + odemeId));
    }

    private static BigDecimal tutar(OgrenciOdemeRequest istek) {
        return istek.odenenTutar() != null ? istek.odenenTutar().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    private void doldur(OgrenciOdeme kayit, OgrenciOdemeRequest istek) {
        BigDecimal odenen = tutar(istek);
        String not = istek.not() != null && !istek.not().isBlank() ? istek.not().trim() : null;
        if (odenen.signum() == 0 && not == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tutar girilmelidir; tutarsız kayıt için açıklama (not) yazılmalıdır");
        }

        OdemeYontemi sekil = null;
        Hesap hesap = null;
        LocalDate tarih = null;
        if (odenen.signum() > 0) {
            if (istek.odemeSekli() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Ödeme yöntemi seçilmelidir");
            }
            sekil = istek.odemeSekli();
            Long mevcutHesapId = kayit.getHesap() != null ? kayit.getHesap().getId() : null;
            hesap = hesapService.coz(istek.hesapId(), sekil, mevcutHesapId);
            tarih = istek.odemeTarihi() != null ? istek.odemeTarihi() : AppZaman.bugun();
        }
        kayit.setOdenenTutar(odenen);
        kayit.setTamam(istek.tamam());
        kayit.setOdemeSekli(sekil);
        kayit.setHesap(hesap);
        kayit.setOdemeTarihi(tarih);
        kayit.setNot(not);
        kayit.setKullanici(mevcutKullanici.getir());
    }

    private OgrenciOdemeYilResponse yanit(Long ogrenciId, int yil) {
        yilKontrol(yil);
        Map<Integer, List<OgrenciOdeme>> kayitlar = odemeRepository.findByOgrenciIdAndYil(ogrenciId, yil).stream()
                .collect(Collectors.groupingBy(OgrenciOdeme::getAy));

        LocalDate bugun = AppZaman.bugun();
        List<OgrenciOdemeAyResponse> aylar = new ArrayList<>();
        BigDecimal toplam = BigDecimal.ZERO;
        for (int ay = 1; ay <= 12; ay++) {
            List<OgrenciOdeme> odemeler = kayitlar.getOrDefault(ay, List.of());
            BigDecimal ayToplami = odemeler.stream().map(OgrenciOdeme::getOdenenTutar).reduce(BigDecimal.ZERO, BigDecimal::add);
            boolean tamam = odemeler.stream().anyMatch(OgrenciOdeme::isTamam);
            toplam = toplam.add(ayToplami);
            aylar.add(new OgrenciOdemeAyResponse(yil, ay,
                    OgrenciOdemeDurumu.hesapla(tamam, ayToplami.signum() > 0, yil, ay, bugun), ayToplami,
                    odemeler.stream().map(OgrenciOdemeService::odemeYaniti).toList()));
        }
        Set<Integer> yillar = new TreeSet<>(odemeRepository.yillar(ogrenciId));
        yillar.add(AppZaman.bugun().getYear());
        yillar.add(yil);
        Ogrenci ogrenci = ogrenciRepository.getReferenceById(ogrenciId);
        return new OgrenciOdemeYilResponse(yil, bugun, aylar, toplam, List.copyOf(yillar), ogrenci.isOdemeAlinir(),
                odemeRepository.existsByOgrenciId(ogrenciId));
    }

    private static OgrenciOdemeResponse odemeYaniti(OgrenciOdeme k) {
        return new OgrenciOdemeResponse(k.getId(), k.getOdenenTutar(), k.isTamam(), k.getOdemeSekli(),
                k.getHesap() != null ? k.getHesap().getId() : null, k.getHesap() != null ? k.getHesap().getAd() : null,
                k.getOdemeTarihi(), k.getNot());
    }

    private static void yilKontrol(int yil) {
        if (yil < 2000 || yil > 2100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Yıl 2000-2100 arasında olmalıdır");
        }
    }

    private static void ayKontrol(int ay) {
        if (ay < 1 || ay > 12) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ay 1-12 arasında olmalıdır");
        }
    }

    private Ogrenci ogrenciGetir(Long id) {
        Ogrenci ogrenci = ogrenciRepository.findByIdAndIsletmeId(id, isletmeContext.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Öğrenci bulunamadı: " + id));
        grupKapsami.kontrol(ogrenci);
        return ogrenci;
    }
}
