package com.msc.halisaha.service;

import com.msc.halisaha.common.AppZaman;
import com.msc.halisaha.common.TurkceSirala;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.common.security.IsletmeContext;
import com.msc.halisaha.dto.AbonelikAyarRequest;
import com.msc.halisaha.dto.AntrenmanaCevirRequest;
import com.msc.halisaha.dto.AbonelikAyarResponse;
import com.msc.halisaha.dto.AbonelikGuncelleRequest;
import com.msc.halisaha.dto.AbonelikIptalOnizlemeResponse;
import com.msc.halisaha.dto.AbonelikIptalRequest;
import com.msc.halisaha.dto.AbonelikOdemeRequest;
import com.msc.halisaha.dto.AbonelikOnizlemeRequest;
import com.msc.halisaha.dto.AbonelikOnizlemeResponse;
import com.msc.halisaha.dto.AbonelikRequest;
import com.msc.halisaha.dto.AbonelikResponse;
import com.msc.halisaha.dto.PageResponse;
import com.msc.halisaha.dto.IadeRequest;
import com.msc.halisaha.entity.Abonelik;
import com.msc.halisaha.entity.AbonelikAyar;
import com.msc.halisaha.entity.AbonelikDurum;
import com.msc.halisaha.entity.AbonelikOdeme;
import com.msc.halisaha.entity.KalemTip;
import com.msc.halisaha.entity.OdemeDurumu;
import com.msc.halisaha.entity.Rezervasyon;
import com.msc.halisaha.entity.RezervasyonDurum;
import com.msc.halisaha.entity.RezervasyonKalem;
import com.msc.halisaha.entity.Rol;
import com.msc.halisaha.entity.Saha;
import com.msc.halisaha.repository.AbonelikAyarRepository;
import com.msc.halisaha.repository.AbonelikRepository;
import com.msc.halisaha.repository.IadeRepository;
import com.msc.halisaha.repository.RezervasyonKalemRepository;
import com.msc.halisaha.repository.RezervasyonOdemeRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Abonelik: sahanın haftanın belli günü ve saatinde, başlangıçtan itibaren 52 hafta boyunca bir müşteriye ayrılması.
 * Kayıtta her hafta için bir rezervasyon oluşturulur (takvimde görünür, çakışma kontrolü normal rezervasyonlarla ortaktır).
 * Abone kayıtta depozito bırakır (abonelik ödemesi olarak alınır, işletmede kalır); haftalık rezervasyonlar haftalık ücretle açılır
 * ve her hafta normal rezervasyon ödemesi gibi tahsil edilir.
 * İptalde kalan depozito ile kalan haftalar oynanır: haftalık ücrete yetecek kadar hafta rezervasyonda kalır (ücreti sıfırlanır, "depozitodan"),
 * diğer haftalar iptal edilir. Bir maça yetmeyen artan tutar iade edilir ya da bir sonraki maça sayılır (müşteri farkı öder).
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
    private final RezervasyonOdemeRepository rezervasyonOdemeRepository;
    private final SahaRepository sahaRepository;
    private final RezervasyonService rezervasyonService;
    private final HesapService hesapService;
    private final IadeRepository iadeRepository;
    private final IadeService iadeService;
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

    /** Sayfalı liste: durum, saha, haftanın günü (1 = Pazartesi ... 7 = Pazar) ve arama (ad soyad / telefon) filtreleriyle; toplamKayit filtrelenmiş abone sayısıdır. */
    public PageResponse<AbonelikResponse> sayfa(AbonelikDurum durum, Long sahaId, Integer haftaGunu, String q, int sayfa, int boyut) {
        sayfa = Math.max(sayfa, 0);
        boyut = Math.min(Math.max(boyut, 1), 200);
        List<AbonelikResponse> tumu = listele(durum, q).stream()
                .filter(a -> sahaId == null || sahaId.equals(a.sahaId()))
                .filter(a -> haftaGunu == null || haftaGunu == a.haftaGunu())
                .toList();
        int bas = Math.min(sayfa * boyut, tumu.size());
        int son = Math.min(bas + boyut, tumu.size());
        int toplamSayfa = (tumu.size() + boyut - 1) / boyut;
        return new PageResponse<>(tumu.subList(bas, son), sayfa, boyut, tumu.size(), toplamSayfa);
    }

    public List<AbonelikResponse> listele(AbonelikDurum durum, String q) {
        LocalDate bugun = AppZaman.bugun();
        String aranan = q == null ? "" : q.trim().toLowerCase(TR);
        Map<Long, BigDecimal> iadeler = new HashMap<>();
        iadeRepository.abonelikToplamlari(isletmeContext.id()).forEach(r -> iadeler.put((Long) r[0], (BigDecimal) r[1]));
        return abonelikRepository.listele(isletmeContext.id(), durum).stream()
                .filter(a -> aranan.isEmpty()
                        || a.getAdSoyad().toLowerCase(TR).contains(aranan)
                        || a.getTelNo().contains(aranan))
                .sorted(TurkceSirala.<Abonelik>alfabetik(Abonelik::getAdSoyad).thenComparing(Abonelik::getId))
                .map(a -> AbonelikResponse.from(a, bugun, List.of(), iadeler.getOrDefault(a.getId(), BigDecimal.ZERO)))
                .toList();
    }

    public AbonelikResponse getir(Long id) {
        Abonelik abonelik = getEntity(id);
        return yanit(abonelik);
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
        boolean antrenman = Boolean.TRUE.equals(request.antrenman());
        BigDecimal depozito = BigDecimal.ZERO.setScale(2);
        BigDecimal haftalik = depozito;
        BigDecimal odenen = depozito;
        LocalDate odemeTarihi = null;
        if (!antrenman) {
            if (request.depozito() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Depozito zorunludur");
            }
            if (request.haftalikUcret() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Haftalık ücret zorunludur");
            }
            if (request.odenenTutar() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Alınan ödeme tutarı zorunludur");
            }
            if (request.odemeSekli() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Ödeme yöntemi seçilmelidir");
            }
            depozito = request.depozito().setScale(2, RoundingMode.HALF_UP);
            haftalik = request.haftalikUcret().setScale(2, RoundingMode.HALF_UP);
            odenen = request.odenenTutar().setScale(2, RoundingMode.HALF_UP);
            if (odenen.compareTo(depozito) > 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Alınan ödeme depozitoyu (" + depozito + ") aşamaz");
            }
            odemeTarihi = odemeTarihi(request.odemeTarihi(), bugun);
        }

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
                .antrenman(antrenman)
                .depozito(depozito)
                .haftalikUcret(haftalik)
                .not(not)
                .kullanici(mevcutKullanici.getir())
                .build();
        if (!antrenman) {
            abonelik.getOdemeler().add(odemeOlustur(abonelik, odenen, request.odemeSekli(), request.hesapId(), odemeTarihi));
        }
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
                    .antrenman(antrenman)
                    .odemeDurumu(antrenman ? OdemeDurumu.ODENDI : OdemeDurumu.ODENMEDI)
                    .odenecekTutar(haftalik)
                    .kayitTarih(simdi)
                    .kullanici(abonelik.getKullanici())
                    .abonelik(abonelik)
                    .build());
        }
        rezervasyonRepository.saveAll(rezervasyonlar);
        BigDecimal kalemTutari = haftalik;
        kalemRepository.saveAll(rezervasyonlar.stream().map(r -> RezervasyonKalem.builder()
                .rezervasyon(r)
                .tip(KalemTip.SAHA)
                .aciklama(saha.getAd() + (antrenman ? " · Antrenman" : " · Abonelik"))
                .miktar(BigDecimal.valueOf(request.sureDakika()).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP))
                .birimFiyat(kalemTutari.divide(BigDecimal.valueOf(request.sureDakika()).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP), 2, RoundingMode.HALF_UP))
                .tutar(kalemTutari)
                .build()).toList());

        return yanit(abonelik);
    }

    /**
     * Ad, telefon, depozito, haftalık ücret ve not güncellenir; gerçekleşmemiş haftalık rezervasyonlarda ad, telefon ve
     * (ödemesi alınmamışsa) haftalık ücret de güncellenir.
     */
    @Transactional
    public AbonelikResponse guncelle(Long id, AbonelikGuncelleRequest request) {
        Abonelik abonelik = getEntity(id);
        if (abonelik.isAntrenman()) {
            return antrenmanGuncelle(abonelik, request);
        }
        if (request.depozito() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Depozito zorunludur");
        }
        if (request.haftalikUcret() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Haftalık ücret zorunludur");
        }
        BigDecimal depozito = request.depozito().setScale(2, RoundingMode.HALF_UP);
        BigDecimal haftalik = request.haftalikUcret().setScale(2, RoundingMode.HALF_UP);
        BigDecimal odenen = odenenToplam(abonelik);
        if (depozito.compareTo(odenen) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Depozito alınan ödemenin (" + odenen + ") altına düşürülemez");
        }
        boolean haftalikDegisti = haftalik.compareTo(abonelik.getHaftalikUcret()) != 0 && abonelik.getDurum() == AbonelikDurum.AKTIF;
        abonelik.setAdSoyad(request.adSoyad().trim());
        abonelik.setTelNo(request.telNo());
        abonelik.setDepozito(depozito);
        abonelik.setHaftalikUcret(haftalik);
        abonelik.setNot(request.not() != null && !request.not().isBlank() ? request.not().trim() : null);

        LocalDateTime simdi = AppZaman.simdi();
        for (Rezervasyon r : rezervasyonRepository.findByAbonelikId(id)) {
            if (r.getDurum() == RezervasyonDurum.AKTIF && !r.getBaslangicTarih().isBefore(simdi)) {
                r.setAdSoyad(abonelik.getAdSoyad());
                r.setTelNo(abonelik.getTelNo());
                if (haftalikDegisti) {
                    rezervasyonService.abonelikSahaTutariBelirle(r.getId(), haftalik, false);
                }
            }
        }
        return yanit(abonelik);
    }

    /** Antrenman aboneliğinde yalnızca ad, telefon ve not değişir (tutar yoktur). */
    private AbonelikResponse antrenmanGuncelle(Abonelik abonelik, AbonelikGuncelleRequest request) {
        abonelik.setAdSoyad(request.adSoyad().trim());
        abonelik.setTelNo(request.telNo());
        abonelik.setNot(request.not() != null && !request.not().isBlank() ? request.not().trim() : null);
        LocalDateTime simdi = AppZaman.simdi();
        for (Rezervasyon r : rezervasyonRepository.findByAbonelikId(abonelik.getId())) {
            if (r.getDurum() == RezervasyonDurum.AKTIF && !r.getBaslangicTarih().isBefore(simdi)) {
                r.setAdSoyad(abonelik.getAdSoyad());
                r.setTelNo(abonelik.getTelNo());
            }
        }
        return yanit(abonelik);
    }

    /**
     * Ücretli (yanlış girilmiş) aboneliği antrenmana çevirir: bugünden itibaren olan haftalık rezervasyonların ücreti sıfırlanır,
     * ödeme durumu "ödendi" olur ve raporlara girmez; geçmiş haftalar olduğu gibi kalır. Aboneliğin depozito ödemeleri varsa
     * yalnızca {@code odemeleriSil} ile silinir (kasadan düşer). İade kaydı, ödemesi alınmış ya da ürün eklenmiş hafta varsa çevrilmez.
     */
    @Transactional
    public AbonelikResponse antrenmanaCevir(Long id, AntrenmanaCevirRequest request) {
        Abonelik abonelik = getEntity(id);
        if (abonelik.isAntrenman()) {
            return yanit(abonelik);
        }
        if (abonelik.getDurum() != AbonelikDurum.AKTIF) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "İptal edilen abonelik antrenmana çevrilemez");
        }
        if (iadeRepository.abonelikToplami(id).signum() > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Bu abonelik için iade kaydı var, antrenmana çevrilemez");
        }
        LocalDateTime bugun = AppZaman.bugun().atStartOfDay();
        List<Rezervasyon> haftalar = rezervasyonRepository.findByAbonelikId(id).stream()
                .filter(r -> r.getDurum() == RezervasyonDurum.AKTIF && !r.getBaslangicTarih().isBefore(bugun))
                .toList();
        for (Rezervasyon r : haftalar) {
            if (rezervasyonOdemeRepository.existsByRezervasyonId(r.getId())) {
                throw new ApiException(HttpStatus.CONFLICT, r.getBaslangicTarih().toLocalDate().format(TARIH)
                        + " tarihli haftanın ödemesi alınmış, aboneliği antrenmana çevirmeden önce bu ödemeyi kontrol edin");
            }
        }
        BigDecimal odenen = odenenToplam(abonelik);
        if (odenen.signum() > 0 && !Boolean.TRUE.equals(request.odemeleriSil())) {
            throw new ApiException(HttpStatus.CONFLICT, "Bu abonelikte " + odenen + " ₺ depozito ödemesi kayıtlı. "
                    + "Antrenmana çevrilirse bu ödeme kayıtları silinir ve kasadan düşer; onay gerekir");
        }
        for (Rezervasyon r : haftalar) {
            rezervasyonService.antrenmanYap(r.getId());
        }
        abonelik.getOdemeler().clear();
        abonelik.setDepozito(BigDecimal.ZERO.setScale(2));
        abonelik.setHaftalikUcret(BigDecimal.ZERO.setScale(2));
        abonelik.setAntrenman(true);
        abonelikRepository.flush();
        return yanit(abonelik);
    }

    /** İptal edilirse ne olacağını hesaplar (iptal ekranı için); hiçbir şeyi değiştirmez. */
    public AbonelikIptalOnizlemeResponse iptalOnizle(Long id) {
        Abonelik abonelik = getEntity(id);
        IptalPlani plan = planla(abonelik);
        BigDecimal haftalik = abonelik.getHaftalikUcret();
        int iptalEdilecek = plan.adaylar().size() - plan.tamHafta() - (plan.kismiHafta() ? 1 : 0);
        return new AbonelikIptalOnizlemeResponse(plan.kalanDepozito(), haftalik, plan.tamHafta(), iptalEdilecek, plan.artan(),
                plan.kismiHafta(), plan.kismiHafta() ? haftalik.subtract(plan.artan()) : null,
                plan.artan().signum() > 0 && !plan.kismiHafta());
    }

    /**
     * Aboneliği iptal eder. Kalan depozito ile kalan haftalar oynanır: haftalık ücrete yetecek kadar (en yakın) hafta rezervasyonda kalır
     * ve ücreti depozitodan karşılanır, diğer haftalar "Abonelik iptali" nedeniyle iptal edilir. Bir maça yetmeyen artan tutar
     * ya iade edilir (iade kaydı oluşur; yalnızca yönetici) ya da bir sonraki maça sayılır: o maçta müşteri yalnızca farkı öder.
     * Geçmiş haftalar ve ödemesi alınmış haftalar olduğu gibi kalır; alınan ödemeler silinmez.
     */
    @Transactional
    public AbonelikResponse iptalEt(Long id, AbonelikIptalRequest request) {
        Abonelik abonelik = getEntity(id);
        if (abonelik.getDurum() != AbonelikDurum.AKTIF) {
            return yanit(abonelik);
        }
        IptalPlani plan = planla(abonelik);
        boolean artanIade = plan.artan().signum() > 0 && (!plan.kismiHafta() || Boolean.TRUE.equals(request.artanIade()));
        if (artanIade) {
            if (mevcutKullanici.getir().getRol() != Rol.ADMIN) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Artan depozito iadesini yalnızca yönetici yapabilir");
            }
            if (request.iadeSekli() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Artan depozito iadesi için iade yöntemi seçilmelidir");
            }
        }

        String neden = request.neden() != null && !request.neden().isBlank() ? request.neden().trim() : null;
        abonelik.setDurum(AbonelikDurum.IPTAL);
        abonelik.setIptalTarih(AppZaman.simdi());
        abonelik.setIptalEden(mevcutKullanici.getir());
        abonelik.setIptalNeden(neden);

        BigDecimal haftalik = abonelik.getHaftalikUcret();
        List<Rezervasyon> adaylar = plan.adaylar();
        for (int i = 0; i < adaylar.size(); i++) {
            Long rezervasyonId = adaylar.get(i).getId();
            if (i < plan.tamHafta()) {
                rezervasyonService.abonelikSahaTutariBelirle(rezervasyonId, BigDecimal.ZERO, true);
            } else if (i == plan.tamHafta() && plan.kismiHafta() && !artanIade) {
                rezervasyonService.abonelikSahaTutariBelirle(rezervasyonId, haftalik.subtract(plan.artan()), true);
            } else {
                rezervasyonService.cancel(rezervasyonId, ABONELIK_IPTALI);
            }
        }
        if (artanIade) {
            iadeService.yap(new IadeRequest(null, null, null, id, plan.artan(), request.iadeSekli(), request.hesapId(), null,
                    "Abonelik iptali: artan depozito iadesi"));
        }
        return yanit(abonelik);
    }

    /** Kalan depozito (alınan - iade), oynanabilecek gelecek haftalar ve artan tutarın dökümü. */
    private IptalPlani planla(Abonelik abonelik) {
        if (abonelik.isAntrenman()) {
            // Ücretsiz: oynanacak hafta yoktur, gelecek haftaların hepsi iptal edilir
            return new IptalPlani(BigDecimal.ZERO, gelecekHaftalar(abonelik), 0, BigDecimal.ZERO, false);
        }
        BigDecimal kalan = odenenToplam(abonelik).subtract(iadeRepository.abonelikToplami(abonelik.getId())).max(BigDecimal.ZERO);
        List<Rezervasyon> adaylar = gelecekHaftalar(abonelik);
        BigDecimal haftalik = abonelik.getHaftalikUcret();
        int tam = (int) Math.min(kalan.divideToIntegralValue(haftalik).longValue(), adaylar.size());
        BigDecimal artan = kalan.subtract(haftalik.multiply(BigDecimal.valueOf(tam)));
        return new IptalPlani(kalan, adaylar, tam, artan, artan.signum() > 0 && tam < adaylar.size());
    }

    /** Henüz oynanmamış, aktif ve ödemesi alınmamış haftalık rezervasyonlar. */
    private List<Rezervasyon> gelecekHaftalar(Abonelik abonelik) {
        LocalDateTime simdi = AppZaman.simdi();
        return rezervasyonRepository.findByAbonelikId(abonelik.getId()).stream()
                .filter(r -> r.getDurum() == RezervasyonDurum.AKTIF && !r.getBaslangicTarih().isBefore(simdi))
                .filter(r -> !rezervasyonOdemeRepository.existsByRezervasyonId(r.getId()))
                .toList();
    }

    private record IptalPlani(BigDecimal kalanDepozito, List<Rezervasyon> adaylar, int tamHafta, BigDecimal artan, boolean kismiHafta) {
    }

    private AbonelikResponse yanit(Abonelik abonelik) {
        return AbonelikResponse.from(abonelik, AppZaman.bugun(), haftalar(abonelik), iadeRepository.abonelikToplami(abonelik.getId()));
    }

    @Transactional
    public AbonelikResponse odemeEkle(Long id, AbonelikOdemeRequest request) {
        Abonelik abonelik = getEntity(id);
        if (abonelik.isAntrenman()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Antrenman aboneliği ücretsizdir, ödeme alınmaz");
        }
        BigDecimal tutar = request.tutar().setScale(2, RoundingMode.HALF_UP);
        BigDecimal kalan = abonelik.getDepozito().subtract(odenenToplam(abonelik));
        if (tutar.compareTo(kalan) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ödeme tutarı kalan depozitoyu (" + kalan.max(BigDecimal.ZERO) + ") aşamaz");
        }
        abonelik.getOdemeler().add(odemeOlustur(abonelik, tutar, request.odemeSekli(), request.hesapId(),
                odemeTarihi(request.odemeTarihi(), AppZaman.bugun())));
        abonelikRepository.flush();   // yeni ödeme satırının id'si yanıtta dolu gelsin
        return yanit(abonelik);
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
        return yanit(abonelik);
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
        List<Rezervasyon> rezervasyonlar = rezervasyonRepository.findByAbonelikId(abonelik.getId());
        Map<Long, BigDecimal> odenenler = new HashMap<>();
        if (!rezervasyonlar.isEmpty()) {
            rezervasyonOdemeRepository.toplamlar(rezervasyonlar.stream().map(Rezervasyon::getId).toList())
                    .forEach(r -> odenenler.put((Long) r[0], (BigDecimal) r[1]));
        }
        return rezervasyonlar.stream().map(r -> {
            BigDecimal odenen = odenenler.getOrDefault(r.getId(), BigDecimal.ZERO);
            return new AbonelikResponse.HaftaResponse(r.getId(), r.getBaslangicTarih(), r.getBitisTarih(), r.getDurum(),
                    r.getOdenecekTutar(), odenen, r.getOdenecekTutar().subtract(odenen).max(BigDecimal.ZERO), r.getOdemeDurumu());
        }).toList();
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
