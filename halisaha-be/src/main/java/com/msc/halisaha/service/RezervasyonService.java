package com.msc.halisaha.service;

import com.msc.halisaha.common.AppZaman;
import com.msc.halisaha.common.TurkceSirala;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.common.security.IsletmeContext;
import com.msc.halisaha.dto.OdemeRequest;
import com.msc.halisaha.dto.OdemeResponse;
import com.msc.halisaha.dto.RezervasyonRequest;
import com.msc.halisaha.dto.RezervasyonResponse;
import com.msc.halisaha.dto.RezervasyonUrunRequest;
import com.msc.halisaha.dto.RezervasyonUrunResponse;
import com.msc.halisaha.entity.KalemTip;
import com.msc.halisaha.entity.Hesap;
import com.msc.halisaha.entity.RezervasyonOdeme;
import com.msc.halisaha.entity.OdemeDurumu;
import com.msc.halisaha.entity.OdemeTipi;
import com.msc.halisaha.entity.OdemeYontemi;
import com.msc.halisaha.entity.Rezervasyon;
import com.msc.halisaha.entity.RezervasyonDurum;
import com.msc.halisaha.entity.RezervasyonKalem;
import com.msc.halisaha.entity.Saha;
import com.msc.halisaha.entity.Urun;
import com.msc.halisaha.repository.RezervasyonOdemeRepository;
import com.msc.halisaha.repository.RezervasyonKalemRepository;
import com.msc.halisaha.repository.RezervasyonRepository;
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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RezervasyonService {

    private final RezervasyonRepository rezervasyonRepository;
    private final SahaRepository sahaRepository;
    private final RezervasyonKalemRepository kalemRepository;
    private final RezervasyonOdemeRepository rezervasyonOdemeRepository;
    private final UrunRepository urunRepository;
    private final StokService stokService;
    private final MevcutKullanici mevcutKullanici;
    private final IsletmeContext isletmeContext;
    private final HesapService hesapService;

    public List<RezervasyonResponse> list(Long sahaId, LocalDate tarih) {
        Long isletmeId = isletmeContext.id();
        List<Rezervasyon> rezervasyonlar;
        if (sahaId != null && tarih != null) {
            rezervasyonlar = rezervasyonRepository.findByIsletmeIdAndSahaIdAndBaslangicTarihGreaterThanEqualAndBaslangicTarihLessThan(
                    isletmeId, sahaId, tarih.atStartOfDay(), tarih.plusDays(1).atStartOfDay());
        } else if (tarih != null) {
            rezervasyonlar = rezervasyonRepository.findByIsletmeIdAndBaslangicTarihGreaterThanEqualAndBaslangicTarihLessThan(
                    isletmeId, tarih.atStartOfDay(), tarih.plusDays(1).atStartOfDay());
        } else if (sahaId != null) {
            rezervasyonlar = rezervasyonRepository.findByIsletmeIdAndSahaId(isletmeId, sahaId);
        } else {
            rezervasyonlar = rezervasyonRepository.findByIsletmeId(isletmeId);
        }
        // Alfabetik (Türkçe); aynı isimde saat sırasına göre
        return toResponses(rezervasyonlar.stream()
                .sorted(TurkceSirala.<Rezervasyon>alfabetik(Rezervasyon::getAdSoyad)
                        .thenComparing(Rezervasyon::getBaslangicTarih)
                        .thenComparing(Rezervasyon::getId))
                .toList());
    }

    public RezervasyonResponse getById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public RezervasyonResponse create(RezervasyonRequest request) {
        validateTarihAraligi(request.baslangicTarih(), request.bitisTarih());
        Saha saha = getSaha(request.sahaId());
        checkOverlap(request.sahaId(), request.baslangicTarih(), request.bitisTarih(), null);

        boolean tamOdeme = Boolean.TRUE.equals(request.tamOdeme());
        BigDecimal kapora = request.kapora() != null && !tamOdeme ? request.kapora().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal sahaTutari = hesaplaSahaUcreti(saha, request.baslangicTarih(), request.bitisTarih());
        if (kapora.compareTo(sahaTutari) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Kapora rezervasyon tutarını (" + sahaTutari + ") aşamaz");
        }

        Rezervasyon rezervasyon = rezervasyonRepository.save(Rezervasyon.builder()
                .isletme(isletmeContext.getir())
                .saha(saha)
                .adSoyad(request.adSoyad())
                .baslangicTarih(request.baslangicTarih())
                .bitisTarih(request.bitisTarih())
                .telNo(request.telNo())
                .kayitTarih(AppZaman.simdi())
                .kullanici(mevcutKullanici.getir())
                .odemeDurumu(OdemeDurumu.ODENMEDI)
                .odenecekTutar(BigDecimal.ZERO)
                .build());

        kalemRepository.save(sahaKalemi(rezervasyon, saha, request.baslangicTarih(), request.bitisTarih()));
        recalculate(rezervasyon);

        OdemeYontemi yontem = request.odemeYontemi() != null ? request.odemeYontemi() : OdemeYontemi.NAKIT;
        if (tamOdeme) {
            odemeKaydet(rezervasyon, OdemeTipi.ODEME, rezervasyon.getOdenecekTutar(), yontem, request.hesapId());
        } else if (kapora.signum() > 0) {
            odemeKaydet(rezervasyon, OdemeTipi.KAPORA, kapora, yontem, request.hesapId());
        }
        return toResponse(rezervasyon);
    }

    @Transactional
    public RezervasyonResponse update(Long id, RezervasyonRequest request) {
        Rezervasyon rezervasyon = getEntity(id);
        validateTarihAraligi(request.baslangicTarih(), request.bitisTarih());
        Saha saha = getSaha(request.sahaId());
        checkOverlap(request.sahaId(), request.baslangicTarih(), request.bitisTarih(), id);

        // Abonelik rezervasyonunun saha ücreti 0'dır (abonelik ücretiyle tahsil edilir); saha ya da süre değişse de yeniden fiyatlanmaz
        boolean fiyatlamaDegisti = rezervasyon.getAbonelik() == null
                && (!rezervasyon.getSaha().getId().equals(saha.getId())
                || dakika(rezervasyon.getBaslangicTarih(), rezervasyon.getBitisTarih())
                != dakika(request.baslangicTarih(), request.bitisTarih()));

        rezervasyon.setSaha(saha);
        rezervasyon.setAdSoyad(request.adSoyad());
        rezervasyon.setBaslangicTarih(request.baslangicTarih());
        rezervasyon.setBitisTarih(request.bitisTarih());
        rezervasyon.setTelNo(request.telNo());

        List<RezervasyonKalem> kalemler = kalemRepository.findByRezervasyonIdOrderByIdAsc(id);
        RezervasyonKalem sahaKalemi = kalemler.stream().filter(k -> k.getTip() == KalemTip.SAHA).findFirst().orElse(null);
        if (sahaKalemi == null) {
            kalemRepository.save(sahaKalemi(rezervasyon, saha, request.baslangicTarih(), request.bitisTarih()));
        } else if (fiyatlamaDegisti) {
            // Saha ya da süre değişti: yeni fiyatla yeniden kalemlendir. Aksi halde eski birim fiyat korunur.
            RezervasyonKalem yeni = sahaKalemi(rezervasyon, saha, request.baslangicTarih(), request.bitisTarih());
            sahaKalemi.setAciklama(yeni.getAciklama());
            sahaKalemi.setMiktar(yeni.getMiktar());
            sahaKalemi.setBirimFiyat(yeni.getBirimFiyat());
            sahaKalemi.setTutar(yeni.getTutar());
        } else {
            sahaKalemi.setAciklama(saha.getAd() + " · " + saatMetni(request.baslangicTarih(), request.bitisTarih()));
        }

        recalculate(rezervasyon);
        return toResponse(rezervasyon);
    }

    @Transactional
    public RezervasyonResponse cancel(Long id) {
        Rezervasyon rezervasyon = getEntity(id);
        if (rezervasyon.getDurum() == RezervasyonDurum.IPTAL) {
            return toResponse(rezervasyon);
        }
        stokIadeEt(rezervasyon, kalemRepository.findByRezervasyonIdOrderByIdAsc(id), "rezervasyon iptali");
        rezervasyon.setDurum(RezervasyonDurum.IPTAL);
        return toResponse(rezervasyon);
    }

    @Transactional
    public void delete(Long id) {
        Rezervasyon rezervasyon = getEntity(id);
        if (rezervasyonOdemeRepository.existsByRezervasyonId(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ödemesi alınmış rezervasyon silinemez, iptal edin");
        }
        List<RezervasyonKalem> kalemler = kalemRepository.findByRezervasyonIdOrderByIdAsc(id);
        if (rezervasyon.getDurum() == RezervasyonDurum.AKTIF) {
            stokIadeEt(rezervasyon, kalemler, "rezervasyon silindi");
        }
        // Kalemler önce silinir; yoksa silinen rezervasyona referans veren yönetilen kalemler flush'ta hata verir
        kalemRepository.deleteAll(kalemler);
        kalemRepository.flush();
        rezervasyonRepository.delete(rezervasyon);
    }

    @Transactional
    public RezervasyonResponse urunEkle(Long rezervasyonId, RezervasyonUrunRequest request) {
        Rezervasyon rezervasyon = getEntity(rezervasyonId);
        if (rezervasyon.getDurum() == RezervasyonDurum.IPTAL) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "İptal edilen rezervasyona ürün eklenemez");
        }
        Urun urun = urunRepository.findByIdAndIsletmeId(request.urunId(), isletmeContext.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ürün bulunamadı: " + request.urunId()));

        stokService.dus(urun, request.miktar(), rezervasyon);

        BigDecimal miktar = BigDecimal.valueOf(request.miktar());
        kalemRepository.save(RezervasyonKalem.builder()
                .rezervasyon(rezervasyon)
                .tip(KalemTip.URUN)
                .urun(urun)
                .stokDusuldu(true)
                .aciklama(urun.getAd())
                .miktar(miktar)
                .birimFiyat(urun.getFiyat())
                .tutar(urun.getFiyat().multiply(miktar).setScale(2, RoundingMode.HALF_UP))
                .build());

        recalculate(rezervasyon);
        return toResponse(rezervasyon);
    }

    @Transactional
    public RezervasyonResponse urunCikar(Long rezervasyonId, Long kalemId) {
        Rezervasyon rezervasyon = getEntity(rezervasyonId);
        if (rezervasyon.getDurum() == RezervasyonDurum.IPTAL) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "İptal edilen rezervasyondan ürün çıkarılamaz");
        }
        RezervasyonKalem kalem = kalemRepository.findById(kalemId)
                .filter(k -> k.getTip() == KalemTip.URUN)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Rezervasyon ürünü bulunamadı: " + kalemId));

        if (!kalem.getRezervasyon().getId().equals(rezervasyonId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bu ürün bu rezervasyona ait değil");
        }

        if (kalem.isStokDusuldu()) {
            stokService.iade(kalem.getUrun().getId(), kalem.getMiktar().intValueExact(), rezervasyon, "ürün çıkarıldı");
        }
        kalemRepository.delete(kalem);
        kalemRepository.flush();
        recalculate(rezervasyon);
        return toResponse(rezervasyon);
    }

    /** Stoktan düşülmüş ürün kalemlerini ürün bazında toplayıp stoğa geri ekler. */
    private void stokIadeEt(Rezervasyon rezervasyon, List<RezervasyonKalem> kalemler, String neden) {
        Map<Long, Integer> iadeler = new LinkedHashMap<>();
        for (RezervasyonKalem kalem : kalemler) {
            if (kalem.getTip() == KalemTip.URUN && kalem.isStokDusuldu()) {
                iadeler.merge(kalem.getUrun().getId(), kalem.getMiktar().intValueExact(), Integer::sum);
                kalem.setStokDusuldu(false);
            }
        }
        iadeler.forEach((urunId, miktar) -> stokService.iade(urunId, miktar, rezervasyon, neden));
    }

    @Transactional
    public RezervasyonResponse odemeEkle(Long rezervasyonId, OdemeRequest request) {
        Rezervasyon rezervasyon = getEntity(rezervasyonId);
        if (request.tip() == OdemeTipi.KAPORA) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Kapora yalnızca rezervasyon kaydı sırasında alınır; sonraki ödemeler kısmi ödeme olarak alınır");
        }
        odemeKaydet(rezervasyon, request.tip(), request.tutar().setScale(2, RoundingMode.HALF_UP), request.yontem(), request.hesapId());
        return toResponse(rezervasyon);
    }

    /**
     * Kısmi ödeme KAPORA (yalnızca rezervasyon kaydı sırasında) ya da KISMI (kayıttan sonra)
     * ile alınır ve kalan tutarı aşamaz; ODEME ise kalan tutarın tamamını kapatır.
     */
    private void odemeKaydet(Rezervasyon rezervasyon, OdemeTipi tip, BigDecimal tutar, OdemeYontemi yontem, Long hesapId) {
        if (rezervasyon.getDurum() == RezervasyonDurum.IPTAL) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "İptal edilen rezervasyon için ödeme alınamaz");
        }
        List<RezervasyonOdeme> mevcutOdemeler = rezervasyonOdemeRepository.findByRezervasyonId(rezervasyon.getId());
        BigDecimal kalan = rezervasyon.getOdenecekTutar().subtract(odenenToplam(mevcutOdemeler));
        if (kalan.signum() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Rezervasyonun ödemesi zaten tamamlanmış");
        }
        if (tutar.compareTo(kalan) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tutar kalan tutarı (" + kalan + ") aşamaz");
        }
        if (tip == OdemeTipi.ODEME && tutar.compareTo(kalan) != 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ödeme, kalan tutarın (" + kalan + ") tamamını kapatmalıdır; kısmi ödeme için kapora/kısmi ödeme alın");
        }

        Hesap hesap = hesapService.coz(hesapId, yontem, null);
        rezervasyonOdemeRepository.save(RezervasyonOdeme.builder()
                .rezervasyon(rezervasyon)
                .tip(tip)
                .tutar(tutar)
                .yontem(yontem)
                .hesap(hesap)
                .odemeTarihi(AppZaman.simdi())
                .kullanici(mevcutKullanici.getir())
                .build());
        rezervasyon.setOdemeYontemi(yontem);
        recalculate(rezervasyon);
    }

    /** Toplam tutarı kalemlerden, ödeme durumunu alınan ödemelerden türetir. */
    private void recalculate(Rezervasyon rezervasyon) {
        BigDecimal toplam = kalemRepository.findByRezervasyonIdOrderByIdAsc(rezervasyon.getId()).stream()
                .map(RezervasyonKalem::getTutar)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal odenen = odenenToplam(rezervasyonOdemeRepository.findByRezervasyonId(rezervasyon.getId()));
        if (odenen.compareTo(toplam) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Rezervasyon tutarı alınan ödemenin (" + odenen + ") altına düşürülemez");
        }
        rezervasyon.setOdenecekTutar(toplam);
        rezervasyon.setOdemeDurumu(OdemeDurumu.hesapla(odenen, toplam));
    }

    private BigDecimal odenenToplam(Collection<RezervasyonOdeme> odemeler) {
        return odemeler.stream().map(RezervasyonOdeme::getTutar).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private RezervasyonKalem sahaKalemi(Rezervasyon rezervasyon, Saha saha, LocalDateTime baslangic, LocalDateTime bitis) {
        long dakika = dakika(baslangic, bitis);
        BigDecimal saat = BigDecimal.valueOf(dakika).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        return RezervasyonKalem.builder()
                .rezervasyon(rezervasyon)
                .tip(KalemTip.SAHA)
                .aciklama(saha.getAd() + " · " + saatMetni(baslangic, bitis))
                .miktar(saat)
                .birimFiyat(saha.getSaatlikUcret())
                .tutar(hesaplaSahaUcreti(saha, baslangic, bitis))
                .build();
    }

    private String saatMetni(LocalDateTime baslangic, LocalDateTime bitis) {
        BigDecimal saat = BigDecimal.valueOf(dakika(baslangic, bitis)).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        return saat.stripTrailingZeros().toPlainString() + " saat";
    }

    private long dakika(LocalDateTime baslangic, LocalDateTime bitis) {
        return Duration.between(baslangic, bitis).toMinutes();
    }

    private BigDecimal hesaplaSahaUcreti(Saha saha, LocalDateTime baslangic, LocalDateTime bitis) {
        BigDecimal saat = BigDecimal.valueOf(dakika(baslangic, bitis)).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
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
        return sahaRepository.findByIdAndIsletmeId(sahaId, isletmeContext.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Saha bulunamadı: " + sahaId));
    }

    private Rezervasyon getEntity(Long id) {
        return rezervasyonRepository.findByIdAndIsletmeId(id, isletmeContext.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Rezervasyon bulunamadı: " + id));
    }

    private RezervasyonResponse toResponse(Rezervasyon rezervasyon) {
        return toResponses(List.of(rezervasyon)).get(0);
    }

    /** Kalem ve ödemeleri tek sorguda toplu çeker (liste ekranlarında N+1 olmasın). */
    private List<RezervasyonResponse> toResponses(List<Rezervasyon> rezervasyonlar) {
        if (rezervasyonlar.isEmpty()) {
            return List.of();
        }
        List<Long> ids = rezervasyonlar.stream().map(Rezervasyon::getId).toList();
        Map<Long, List<RezervasyonKalem>> kalemler = kalemRepository.findByRezervasyonIdInOrderByIdAsc(ids).stream()
                .collect(Collectors.groupingBy(k -> k.getRezervasyon().getId()));
        Map<Long, List<RezervasyonOdeme>> odemeler = rezervasyonOdemeRepository.findByRezervasyonIdIn(ids).stream()
                .collect(Collectors.groupingBy(o -> o.getRezervasyon().getId()));

        return rezervasyonlar.stream()
                .map(r -> toResponse(r, kalemler.getOrDefault(r.getId(), List.of()), odemeler.getOrDefault(r.getId(), List.of())))
                .toList();
    }

    private RezervasyonResponse toResponse(Rezervasyon r, List<RezervasyonKalem> kalemler, List<RezervasyonOdeme> odemeler) {
        BigDecimal sahaTutar = toplam(kalemler, k -> k.getTip() == KalemTip.SAHA, RezervasyonKalem::getTutar);
        BigDecimal odenen = odenenToplam(odemeler);
        BigDecimal kapora = toplam(odemeler, o -> o.getTip() == OdemeTipi.KAPORA, RezervasyonOdeme::getTutar);

        List<RezervasyonUrunResponse> urunler = kalemler.stream()
                .filter(k -> k.getTip() == KalemTip.URUN)
                .map(k -> new RezervasyonUrunResponse(
                        k.getId(),
                        k.getUrun().getId(),
                        k.getAciklama(),
                        k.getMiktar().intValue(),
                        k.getBirimFiyat(),
                        k.getTutar()))
                .toList();

        List<OdemeResponse> odemeResponses = odemeler.stream()
                .map(o -> new OdemeResponse(
                        o.getId(),
                        o.getTip(),
                        o.getTutar(),
                        o.getYontem(),
                        o.getOdemeTarihi(),
                        o.getKullanici() != null ? o.getKullanici().getAdSoyad() : null,
                        o.getHesap() != null ? o.getHesap().getId() : null,
                        o.getHesap() != null ? o.getHesap().getAd() : null))
                .toList();

        return new RezervasyonResponse(
                r.getId(),
                r.getSaha().getId(),
                r.getSaha().getAd(),
                r.getAdSoyad(),
                r.getBaslangicTarih(),
                r.getBitisTarih(),
                r.getDurum(),
                r.getTelNo(),
                r.getOdemeYontemi(),
                r.getOdemeDurumu(),
                r.getKayitTarih(),
                r.getKullanici() != null ? r.getKullanici().getAdSoyad() : null,
                r.getOdenecekTutar(),
                sahaTutar,
                odenen,
                r.getOdenecekTutar().subtract(odenen).max(BigDecimal.ZERO),
                kapora,
                urunler,
                odemeResponses,
                r.getAbonelik() != null ? r.getAbonelik().getId() : null
        );
    }

    private <T> BigDecimal toplam(Collection<T> items, Predicate<T> filtre, Function<T, BigDecimal> deger) {
        return items.stream().filter(filtre).map(deger).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
