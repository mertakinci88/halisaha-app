package com.msc.halisaha.service;

import com.msc.halisaha.common.AppZaman;
import com.msc.halisaha.common.TurkceSirala;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.dto.OdemeRequest;
import com.msc.halisaha.dto.OdemeResponse;
import com.msc.halisaha.dto.RezervasyonRequest;
import com.msc.halisaha.dto.RezervasyonResponse;
import com.msc.halisaha.dto.RezervasyonUrunRequest;
import com.msc.halisaha.dto.RezervasyonUrunResponse;
import com.msc.halisaha.entity.KalemTip;
import com.msc.halisaha.entity.Odeme;
import com.msc.halisaha.entity.OdemeDurumu;
import com.msc.halisaha.entity.OdemeTipi;
import com.msc.halisaha.entity.OdemeYontemi;
import com.msc.halisaha.entity.Rezervasyon;
import com.msc.halisaha.entity.RezervasyonDurum;
import com.msc.halisaha.entity.RezervasyonKalem;
import com.msc.halisaha.entity.Saha;
import com.msc.halisaha.entity.Urun;
import com.msc.halisaha.repository.OdemeRepository;
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
    private final OdemeRepository odemeRepository;
    private final UrunRepository urunRepository;
    private final StokService stokService;
    private final MevcutKullanici mevcutKullanici;

    public List<RezervasyonResponse> list(Long sahaId, LocalDate tarih) {
        List<Rezervasyon> rezervasyonlar;
        if (sahaId != null && tarih != null) {
            rezervasyonlar = rezervasyonRepository.findBySahaIdAndBaslangicTarihGreaterThanEqualAndBaslangicTarihLessThan(
                    sahaId, tarih.atStartOfDay(), tarih.plusDays(1).atStartOfDay());
        } else if (tarih != null) {
            rezervasyonlar = rezervasyonRepository.findByBaslangicTarihGreaterThanEqualAndBaslangicTarihLessThan(
                    tarih.atStartOfDay(), tarih.plusDays(1).atStartOfDay());
        } else if (sahaId != null) {
            rezervasyonlar = rezervasyonRepository.findBySahaId(sahaId);
        } else {
            rezervasyonlar = rezervasyonRepository.findAll();
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

        BigDecimal kapora = request.kapora() != null ? request.kapora().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal sahaTutari = hesaplaSahaUcreti(saha, request.baslangicTarih(), request.bitisTarih());
        if (kapora.compareTo(sahaTutari) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Kapora rezervasyon tutarını (" + sahaTutari + ") aşamaz");
        }

        Rezervasyon rezervasyon = rezervasyonRepository.save(Rezervasyon.builder()
                .saha(saha)
                .adSoyad(request.adSoyad())
                .baslangicTarih(request.baslangicTarih())
                .bitisTarih(request.bitisTarih())
                .telNo(request.telNo())
                .odemeDurumu(OdemeDurumu.ODENMEDI)
                .odenecekTutar(BigDecimal.ZERO)
                .build());

        kalemRepository.save(sahaKalemi(rezervasyon, saha, request.baslangicTarih(), request.bitisTarih()));
        recalculate(rezervasyon);

        if (kapora.signum() > 0) {
            odemeKaydet(rezervasyon, OdemeTipi.KAPORA, kapora,
                    request.odemeYontemi() != null ? request.odemeYontemi() : OdemeYontemi.NAKIT);
        }
        return toResponse(rezervasyon);
    }

    @Transactional
    public RezervasyonResponse update(Long id, RezervasyonRequest request) {
        Rezervasyon rezervasyon = getEntity(id);
        validateTarihAraligi(request.baslangicTarih(), request.bitisTarih());
        Saha saha = getSaha(request.sahaId());
        checkOverlap(request.sahaId(), request.baslangicTarih(), request.bitisTarih(), id);

        boolean fiyatlamaDegisti = !rezervasyon.getSaha().getId().equals(saha.getId())
                || dakika(rezervasyon.getBaslangicTarih(), rezervasyon.getBitisTarih())
                != dakika(request.baslangicTarih(), request.bitisTarih());

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
        if (odemeRepository.existsByRezervasyonId(id)) {
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
        Urun urun = urunRepository.findById(request.urunId())
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
        odemeKaydet(rezervasyon, request.tip(), request.tutar().setScale(2, RoundingMode.HALF_UP), request.yontem());
        return toResponse(rezervasyon);
    }

    /**
     * Kısmi ödeme KAPORA (ilk kez, rezervasyon başına bir tane) ya da KISMI (kapora alındıktan sonra)
     * ile alınır ve kalan tutarı aşamaz; ODEME ise kalan tutarın tamamını kapatır.
     */
    private void odemeKaydet(Rezervasyon rezervasyon, OdemeTipi tip, BigDecimal tutar, OdemeYontemi yontem) {
        if (rezervasyon.getDurum() == RezervasyonDurum.IPTAL) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "İptal edilen rezervasyon için ödeme alınamaz");
        }
        List<Odeme> mevcutOdemeler = odemeRepository.findByRezervasyonId(rezervasyon.getId());
        BigDecimal kalan = rezervasyon.getOdenecekTutar().subtract(odenenToplam(mevcutOdemeler));
        if (kalan.signum() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Rezervasyonun ödemesi zaten tamamlanmış");
        }
        if (tutar.compareTo(kalan) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tutar kalan tutarı (" + kalan + ") aşamaz");
        }
        boolean kaporaAlindi = mevcutOdemeler.stream().anyMatch(o -> o.getTip() == OdemeTipi.KAPORA);
        if (tip == OdemeTipi.KAPORA && kaporaAlindi) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bu rezervasyon için kapora zaten alınmış; kısmi ödeme alın");
        }
        if (tip == OdemeTipi.KISMI && !kaporaAlindi) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Kısmi ödeme için önce kapora alınmalıdır");
        }
        if (tip == OdemeTipi.ODEME && tutar.compareTo(kalan) != 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ödeme, kalan tutarın (" + kalan + ") tamamını kapatmalıdır; kısmi ödeme için kapora/kısmi ödeme alın");
        }

        odemeRepository.save(Odeme.builder()
                .rezervasyon(rezervasyon)
                .tip(tip)
                .tutar(tutar)
                .yontem(yontem)
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
        BigDecimal odenen = odenenToplam(odemeRepository.findByRezervasyonId(rezervasyon.getId()));
        if (odenen.compareTo(toplam) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Rezervasyon tutarı alınan ödemenin (" + odenen + ") altına düşürülemez");
        }
        rezervasyon.setOdenecekTutar(toplam);
        rezervasyon.setOdemeDurumu(OdemeDurumu.hesapla(odenen, toplam));
    }

    private BigDecimal odenenToplam(Collection<Odeme> odemeler) {
        return odemeler.stream().map(Odeme::getTutar).reduce(BigDecimal.ZERO, BigDecimal::add);
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
        return sahaRepository.findById(sahaId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Saha bulunamadı: " + sahaId));
    }

    private Rezervasyon getEntity(Long id) {
        return rezervasyonRepository.findById(id)
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
        Map<Long, List<Odeme>> odemeler = odemeRepository.findByRezervasyonIdIn(ids).stream()
                .collect(Collectors.groupingBy(o -> o.getRezervasyon().getId()));

        return rezervasyonlar.stream()
                .map(r -> toResponse(r, kalemler.getOrDefault(r.getId(), List.of()), odemeler.getOrDefault(r.getId(), List.of())))
                .toList();
    }

    private RezervasyonResponse toResponse(Rezervasyon r, List<RezervasyonKalem> kalemler, List<Odeme> odemeler) {
        BigDecimal sahaTutar = toplam(kalemler, k -> k.getTip() == KalemTip.SAHA, RezervasyonKalem::getTutar);
        BigDecimal odenen = odenenToplam(odemeler);
        BigDecimal kapora = toplam(odemeler, o -> o.getTip() == OdemeTipi.KAPORA, Odeme::getTutar);

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
                        o.getKullanici() != null ? o.getKullanici().getAdSoyad() : null))
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
                r.getOdenecekTutar(),
                sahaTutar,
                odenen,
                r.getOdenecekTutar().subtract(odenen).max(BigDecimal.ZERO),
                kapora,
                urunler,
                odemeResponses
        );
    }

    private <T> BigDecimal toplam(Collection<T> items, Predicate<T> filtre, Function<T, BigDecimal> deger) {
        return items.stream().filter(filtre).map(deger).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
