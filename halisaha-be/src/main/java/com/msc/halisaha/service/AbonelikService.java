package com.msc.halisaha.service;

import com.msc.halisaha.common.AppZaman;
import com.msc.halisaha.common.TurkceSirala;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.common.security.IsletmeContext;
import com.msc.halisaha.dto.AbonelikAyarRequest;
import com.msc.halisaha.dto.AbonelikAyarResponse;
import com.msc.halisaha.dto.AbonelikGuncelleRequest;
import com.msc.halisaha.dto.AbonelikOdemeRequest;
import com.msc.halisaha.dto.AbonelikOnizlemeRequest;
import com.msc.halisaha.dto.AbonelikOnizlemeResponse;
import com.msc.halisaha.dto.AbonelikRequest;
import com.msc.halisaha.dto.AbonelikResponse;
import com.msc.halisaha.entity.Abonelik;
import com.msc.halisaha.entity.AbonelikAyar;
import com.msc.halisaha.entity.AbonelikDurum;
import com.msc.halisaha.entity.AbonelikOdeme;
import com.msc.halisaha.entity.KalemTip;
import com.msc.halisaha.entity.OdemeDurumu;
import com.msc.halisaha.entity.Rezervasyon;
import com.msc.halisaha.entity.RezervasyonDurum;
import com.msc.halisaha.entity.RezervasyonKalem;
import com.msc.halisaha.entity.Saha;
import com.msc.halisaha.repository.AbonelikAyarRepository;
import com.msc.halisaha.repository.AbonelikRepository;
import com.msc.halisaha.repository.IadeRepository;
import com.msc.halisaha.repository.RezervasyonKalemRepository;
import com.msc.halisaha.repository.RezervasyonRepository;
import com.msc.halisaha.repository.SahaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Abonelik: sahanın haftanın belli günü ve saatinde, başlangıçtan itibaren 52 hafta boyunca bir müşteriye ayrılması.
 * Kayıtta her hafta için bir rezervasyon oluşturulur (takvimde görünür, çakışma kontrolü normal rezervasyonlarla ortaktır);
 * bu rezervasyonların saha ücreti 0'dır, abonelik tutarı abonelik ödemeleriyle tahsil edilir.
 * Abonelik iptalinde henüz gerçekleşmemiş haftalık rezervasyonlar iptal edilir.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AbonelikService {

    /** Bir abonelik 52 haftadır (1 yıl 52 hafta + 1 gün olduğundan haftalık sayım bazı günlerde 53'e çıkmasın). */
    static final int HAFTA_SAYISI = 52;
    private static final Locale TR = Locale.forLanguageTag("tr-TR");
    private static final DateTimeFormatter TARIH = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final int KAPANIS_DAKIKA = 26 * 60;
    /** Abonelik iptalinde iptal edilen haftalık rezervasyonların nedeni (raporlarda ayrı sayılır). */
    public static final String ABONELIK_IPTALI = "Abonelik iptali";
    private static final int GOSTERILEN_CAKISMA = 5;

    private final AbonelikRepository abonelikRepository;
    private final AbonelikAyarRepository ayarRepository;
    private final RezervasyonRepository rezervasyonRepository;
    private final RezervasyonKalemRepository kalemRepository;
    private final SahaRepository sahaRepository;
    private final RezervasyonService rezervasyonService;
    private final HesapService hesapService;
    private final IadeRepository iadeRepository;
    private final IsletmeContext isletmeContext;
    private final MevcutKullanici mevcutKullanici;

    public AbonelikAyarResponse ayar() {
        return new AbonelikAyarResponse(ayarRepository.findById(isletmeContext.id())
                .map(AbonelikAyar::getStandartUcret).orElse(null));
    }

    @Transactional
    public AbonelikAyarResponse ayarKaydet(AbonelikAyarRequest request) {
        Long isletmeId = isletmeContext.id();
        AbonelikAyar ayar = ayarRepository.findById(isletmeId).orElseGet(() -> new AbonelikAyar(isletmeId, BigDecimal.ZERO));
        ayar.setStandartUcret(request.standartUcret().setScale(2, RoundingMode.HALF_UP));
        ayarRepository.save(ayar);
        return new AbonelikAyarResponse(ayar.getStandartUcret());
    }

    public List<AbonelikResponse> listele(AbonelikDurum durum, String q) {
        LocalDate bugun = AppZaman.bugun();
        String aranan = q == null ? "" : q.trim().toLowerCase(TR);
        return abonelikRepository.listele(isletmeContext.id(), durum).stream()
                .filter(a -> aranan.isEmpty()
                        || a.getAdSoyad().toLowerCase(TR).contains(aranan)
                        || a.getTelNo().contains(aranan))
                .sorted(TurkceSirala.<Abonelik>alfabetik(Abonelik::getAdSoyad).thenComparing(Abonelik::getId))
                .map(a -> AbonelikResponse.from(a, bugun, List.of()))
                .toList();
    }

    public AbonelikResponse getir(Long id) {
        Abonelik abonelik = getEntity(id);
        return AbonelikResponse.from(abonelik, AppZaman.bugun(), haftalar(abonelik));
    }

    /** Kayıttan önce: oluşacak rezervasyon sayısı ve çakışan tarihler. */
    public AbonelikOnizlemeResponse onizle(AbonelikOnizlemeRequest request) {
        Saha saha = getSaha(request.sahaId());
        saatiDogrula(request.baslangicDakika(), request.sureDakika());
        List<LocalDate> tarihler = tarihler(request.baslangicTarih(), request.haftaGunu());
        LocalDate bitis = tarihler.get(tarihler.size() - 1);
        return new AbonelikOnizlemeResponse(request.baslangicTarih(), bitis, tarihler.size(),
                tarihler.isEmpty() ? null : tarihler.get(0), tarihler.isEmpty() ? null : tarihler.get(tarihler.size() - 1),
                cakismalar(saha, tarihler, request.baslangicDakika(), request.sureDakika()));
    }

    @Transactional
    public AbonelikResponse olustur(AbonelikRequest request) {
        Saha saha = getSaha(request.sahaId());
        saatiDogrula(request.baslangicDakika(), request.sureDakika());
        LocalDate bugun = AppZaman.bugun();
        if (request.baslangicTarih().isBefore(bugun)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Başlangıç tarihi geçmişte olamaz");
        }
        BigDecimal ucret = request.ucret().setScale(2, RoundingMode.HALF_UP);
        BigDecimal odenen = request.odenenTutar().setScale(2, RoundingMode.HALF_UP);
        if (odenen.compareTo(ucret) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Alınan ödeme abonelik ücretini (" + ucret + ") aşamaz");
        }
        LocalDate odemeTarihi = odemeTarihi(request.odemeTarihi(), bugun);

        List<LocalDate> tarihler = tarihler(request.baslangicTarih(), request.haftaGunu());
        LocalDate bitis = tarihler.get(tarihler.size() - 1);
        List<AbonelikOnizlemeResponse.Cakisma> cakismalar = cakismalar(saha, tarihler, request.baslangicDakika(), request.sureDakika());
        if (!cakismalar.isEmpty()) {
            String liste = cakismalar.stream().limit(GOSTERILEN_CAKISMA)
                    .map(c -> c.tarih().format(TARIH) + " (" + c.adSoyad() + ")").collect(Collectors.joining(", "));
            throw new ApiException(HttpStatus.CONFLICT, "Seçilen gün ve saatte " + cakismalar.size() + " tarihte bu saha için rezervasyon var: "
                    + liste + (cakismalar.size() > GOSTERILEN_CAKISMA ? " ve diğerleri" : "")
                    + ". Önce bu rezervasyonları iptal edin ya da farklı bir gün/saat seçin.");
        }

        String not = request.not() != null && !request.not().isBlank() ? request.not().trim() : null;
        Abonelik abonelik = Abonelik.builder()
                .isletme(isletmeContext.getir())
                .saha(saha)
                .adSoyad(request.adSoyad().trim())
                .telNo(request.telNo())
                .haftaGunu(request.haftaGunu())
                .baslangicDakika(request.baslangicDakika())
                .sureDakika(request.sureDakika())
                .baslangicTarih(request.baslangicTarih())
                .bitisTarih(bitis)
                .durum(AbonelikDurum.AKTIF)
                .ucret(ucret)
                .not(not)
                .kullanici(mevcutKullanici.getir())
                .build();
        abonelik.getOdemeler().add(odemeOlustur(abonelik, odenen, request.odemeSekli(), request.hesapId(), odemeTarihi));
        abonelikRepository.save(abonelik);

        LocalDateTime simdi = AppZaman.simdi();
        List<Rezervasyon> rezervasyonlar = new ArrayList<>();
        for (LocalDate tarih : tarihler) {
            LocalDateTime baslangic = baslangic(tarih, request.baslangicDakika());
            rezervasyonlar.add(Rezervasyon.builder()
                    .isletme(abonelik.getIsletme())
                    .saha(saha)
                    .adSoyad(abonelik.getAdSoyad())
                    .telNo(abonelik.getTelNo())
                    .baslangicTarih(baslangic)
                    .bitisTarih(baslangic.plusMinutes(request.sureDakika()))
                    .durum(RezervasyonDurum.AKTIF)
                    .odemeDurumu(OdemeDurumu.ODENMEDI)
                    .odenecekTutar(BigDecimal.ZERO)
                    .kayitTarih(simdi)
                    .kullanici(abonelik.getKullanici())
                    .abonelik(abonelik)
                    .build());
        }
        rezervasyonRepository.saveAll(rezervasyonlar);
        kalemRepository.saveAll(rezervasyonlar.stream().map(r -> RezervasyonKalem.builder()
                .rezervasyon(r)
                .tip(KalemTip.SAHA)
                .aciklama(saha.getAd() + " · Abonelik")
                .miktar(BigDecimal.valueOf(request.sureDakika()).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP))
                .birimFiyat(BigDecimal.ZERO)
                .tutar(BigDecimal.ZERO)
                .build()).toList());

        return AbonelikResponse.from(abonelik, bugun, haftalar(abonelik));
    }

    /** Ad, telefon, ücret ve not güncellenir; gerçekleşmemiş haftalık rezervasyonlarda da ad ve telefon güncellenir. */
    @Transactional
    public AbonelikResponse guncelle(Long id, AbonelikGuncelleRequest request) {
        Abonelik abonelik = getEntity(id);
        BigDecimal ucret = request.ucret().setScale(2, RoundingMode.HALF_UP);
        BigDecimal odenen = odenenToplam(abonelik);
        if (ucret.compareTo(odenen) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Abonelik ücreti alınan ödemenin (" + odenen + ") altına düşürülemez");
        }
        abonelik.setAdSoyad(request.adSoyad().trim());
        abonelik.setTelNo(request.telNo());
        abonelik.setUcret(ucret);
        abonelik.setNot(request.not() != null && !request.not().isBlank() ? request.not().trim() : null);

        LocalDateTime simdi = AppZaman.simdi();
        for (Rezervasyon r : rezervasyonRepository.findByAbonelikId(id)) {
            if (r.getDurum() == RezervasyonDurum.AKTIF && !r.getBaslangicTarih().isBefore(simdi)) {
                r.setAdSoyad(abonelik.getAdSoyad());
                r.setTelNo(abonelik.getTelNo());
            }
        }
        return AbonelikResponse.from(abonelik, AppZaman.bugun(), haftalar(abonelik));
    }

    /** Aboneliği iptal eder; henüz gerçekleşmemiş haftalık rezervasyonlar iptal edilir, geçmiş haftalar kalır. Alınan ödemeler silinmez. */
    @Transactional
    public AbonelikResponse iptalEt(Long id) {
        return iptalEt(id, null);
    }

    /** İptal eden kullanıcı, zaman ve (varsa) neden kaydedilir; iptal edilen haftalık rezervasyonlar "Abonelik iptali" nedeniyle işaretlenir. */
    @Transactional
    public AbonelikResponse iptalEt(Long id, String neden) {
        Abonelik abonelik = getEntity(id);
        if (abonelik.getDurum() == AbonelikDurum.AKTIF) {
            abonelik.setDurum(AbonelikDurum.IPTAL);
            abonelik.setIptalTarih(AppZaman.simdi());
            abonelik.setIptalEden(mevcutKullanici.getir());
            abonelik.setIptalNeden(neden != null && !neden.isBlank() ? neden.trim() : null);
            LocalDateTime simdi = AppZaman.simdi();
            for (Rezervasyon r : rezervasyonRepository.findByAbonelikId(id)) {
                if (r.getDurum() == RezervasyonDurum.AKTIF && !r.getBaslangicTarih().isBefore(simdi)) {
                    rezervasyonService.cancel(r.getId(), ABONELIK_IPTALI);
                }
            }
        }
        return AbonelikResponse.from(abonelik, AppZaman.bugun(), haftalar(abonelik));
    }

    @Transactional
    public AbonelikResponse odemeEkle(Long id, AbonelikOdemeRequest request) {
        Abonelik abonelik = getEntity(id);
        BigDecimal tutar = request.tutar().setScale(2, RoundingMode.HALF_UP);
        BigDecimal kalan = abonelik.getUcret().subtract(odenenToplam(abonelik));
        if (tutar.compareTo(kalan) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ödeme tutarı kalan tutarı (" + kalan.max(BigDecimal.ZERO) + ") aşamaz");
        }
        abonelik.getOdemeler().add(odemeOlustur(abonelik, tutar, request.odemeSekli(), request.hesapId(),
                odemeTarihi(request.odemeTarihi(), AppZaman.bugun())));
        abonelikRepository.flush();   // yeni ödeme satırının id'si yanıtta dolu gelsin
        return AbonelikResponse.from(abonelik, AppZaman.bugun(), haftalar(abonelik));
    }

    /** Yanlış girilen ödemeyi siler; aboneliğin en az bir ödemesi kalmalıdır. */
    @Transactional
    public AbonelikResponse odemeSil(Long id, Long odemeId) {
        Abonelik abonelik = getEntity(id);
        AbonelikOdeme odeme = abonelik.getOdemeler().stream().filter(o -> o.getId().equals(odemeId)).findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ödeme bulunamadı: " + odemeId));
        if (abonelik.getOdemeler().size() == 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Aboneliğin en az bir ödemesi olmalıdır");
        }
        if (odenenToplam(abonelik).subtract(odeme.getTutar()).compareTo(iadeRepository.abonelikToplami(id)) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bu abonelik için iade kaydı var; ödeme silinirse ödenen tutar iade tutarının altına düşer");
        }
        abonelik.getOdemeler().remove(odeme);
        return AbonelikResponse.from(abonelik, AppZaman.bugun(), haftalar(abonelik));
    }

    private AbonelikOdeme odemeOlustur(Abonelik abonelik, BigDecimal tutar, com.msc.halisaha.entity.OdemeYontemi sekil, Long hesapId, LocalDate tarih) {
        return AbonelikOdeme.builder()
                .abonelik(abonelik)
                .tutar(tutar)
                .odemeSekli(sekil)
                .hesap(hesapService.coz(hesapId, sekil, null))
                .odemeTarihi(tarih)
                .kullanici(mevcutKullanici.getir())
                .kayitTarih(AppZaman.simdi())
                .build();
    }

    private LocalDate odemeTarihi(LocalDate istenen, LocalDate bugun) {
        if (istenen == null) {
            return bugun;
        }
        if (istenen.isAfter(bugun)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ödeme tarihi ileri bir tarih olamaz");
        }
        return istenen;
    }

    private BigDecimal odenenToplam(Abonelik abonelik) {
        return abonelik.getOdemeler().stream().map(AbonelikOdeme::getTutar).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<AbonelikResponse.HaftaResponse> haftalar(Abonelik abonelik) {
        return rezervasyonRepository.findByAbonelikId(abonelik.getId()).stream()
                .map(r -> new AbonelikResponse.HaftaResponse(r.getId(), r.getBaslangicTarih(), r.getBitisTarih(), r.getDurum()))
                .toList();
    }

    /** İlk tarih, başlangıçtan itibaren (başlangıç dahil) seçilen güne denk gelen ilk gündür; ardından haftalık, toplam {@link #HAFTA_SAYISI} hafta. */
    private List<LocalDate> tarihler(LocalDate baslangic, int haftaGunu) {
        List<LocalDate> tarihler = new ArrayList<>(HAFTA_SAYISI);
        LocalDate tarih = baslangic.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(haftaGunu)));
        for (int i = 0; i < HAFTA_SAYISI; i++) {
            tarihler.add(tarih);
            tarih = tarih.plusWeeks(1);
        }
        return tarihler;
    }

    /** Gün başından itibaren dakika: 720 = 12:00, 1440 = ertesi gün 00:00. */
    private LocalDateTime baslangic(LocalDate tarih, int dakika) {
        return tarih.atStartOfDay().plusMinutes(dakika);
    }

    private List<AbonelikOnizlemeResponse.Cakisma> cakismalar(Saha saha, List<LocalDate> tarihler, int dakika, int sure) {
        List<AbonelikOnizlemeResponse.Cakisma> cakismalar = new ArrayList<>();
        for (LocalDate tarih : tarihler) {
            LocalDateTime baslangic = baslangic(tarih, dakika);
            List<Rezervasyon> cakisan = rezervasyonRepository.findOverlapping(saha.getId(), baslangic, baslangic.plusMinutes(sure), null);
            if (!cakisan.isEmpty()) {
                cakismalar.add(new AbonelikOnizlemeResponse.Cakisma(tarih, cakisan.get(0).getAdSoyad()));
            }
        }
        return cakismalar;
    }

    private void saatiDogrula(int baslangicDakika, int sureDakika) {
        if (baslangicDakika % 30 != 0 || sureDakika % 30 != 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Saat ve süre tam saat ya da buçuklu olmalıdır (örn. 20:00, 20:30)");
        }
        if (baslangicDakika + sureDakika > KAPANIS_DAKIKA) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Seçilen saat ve süre kapanış saatini (02:00) aşıyor");
        }
    }

    private Saha getSaha(Long sahaId) {
        return sahaRepository.findByIdAndIsletmeId(sahaId, isletmeContext.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Saha bulunamadı: " + sahaId));
    }

    private Abonelik getEntity(Long id) {
        return abonelikRepository.bul(id, isletmeContext.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Abonelik bulunamadı: " + id));
    }
}
