package com.msc.halisaha.service;

import com.msc.halisaha.common.AppZaman;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.common.security.IsletmeContext;
import com.msc.halisaha.dto.EnvanterSatisRequest;
import com.msc.halisaha.dto.EnvanterSatisResponse;
import com.msc.halisaha.dto.EnvanterTeslimBekleyenResponse;
import com.msc.halisaha.entity.Envanter;
import com.msc.halisaha.entity.EnvanterBeden;
import com.msc.halisaha.entity.EnvanterSatis;
import com.msc.halisaha.entity.EnvanterSatisKalem;
import com.msc.halisaha.entity.EnvanterSatisOdeme;
import com.msc.halisaha.entity.EnvanterSet;
import com.msc.halisaha.entity.EnvanterTur;
import com.msc.halisaha.entity.Ogrenci;
import com.msc.halisaha.repository.EnvanterRepository;
import com.msc.halisaha.repository.EnvanterSatisKalemRepository;
import com.msc.halisaha.repository.EnvanterSatisRepository;
import com.msc.halisaha.repository.EnvanterSetRepository;
import com.msc.halisaha.repository.IadeRepository;
import com.msc.halisaha.repository.OgrenciRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Öğrenciye envanter seti satışı. Set, işletmede kaydı olan envanter türlerinden oluşur (her türden en fazla bir kalem);
 * satış güncellenirken envanter eklenebilir / çıkarılabilir (çıkarılan teslim edilmişse stoğu geri döner); beden, ödendi ve teslim de güncellenir.
 * Stok yalnızca teslim edilen kalem için düşer: teslim işaretlenip kaydedilince düşer, teslim geri alınırsa
 * ya da satış silinirse geri döner. Stoğu biten envanter satılabilir ama teslim edilemez (409).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnvanterSatisService {

    private final EnvanterSatisRepository satisRepository;
    private final EnvanterSatisKalemRepository kalemRepository;
    private final EnvanterRepository envanterRepository;
    private final EnvanterSetRepository setRepository;
    private final OgrenciRepository ogrenciRepository;
    private final EnvanterStokService stokService;
    private final HesapService hesapService;
    private final IadeRepository iadeRepository;
    private final IsletmeContext isletmeContext;
    private final MevcutKullanici mevcutKullanici;

    public List<EnvanterSatisResponse> listele(Long ogrenciId) {
        Long isletmeId = isletmeContext.id();
        ogrenciGetir(ogrenciId, isletmeId);
        return satisRepository.listele(isletmeId, ogrenciId).stream().map(EnvanterSatisResponse::from).toList();
    }

    /** Panel için: ödemesi alınmış, teslim edilmemiş envanterler (öğrenciye göre gruplu, en eski satış önce). */
    public EnvanterTeslimBekleyenResponse teslimBekleyenler() {
        return EnvanterTeslimBekleyenResponse.from(kalemRepository.teslimBekleyenler(isletmeContext.id()));
    }

    @Transactional
    public EnvanterSatisResponse olustur(Long ogrenciId, EnvanterSatisRequest request) {
        Long isletmeId = isletmeContext.id();
        Ogrenci ogrenci = ogrenciGetir(ogrenciId, isletmeId);
        Map<EnvanterTur, EnvanterSatisRequest.Kalem> istekler = kalemleriTopla(request);
        Map<EnvanterTur, Envanter> envanterler = envanterleriBul(isletmeId, istekler);
        EnvanterSet set = setBul(isletmeId, request.setId(), istekler);

        EnvanterSatis satis = EnvanterSatis.builder()
                .isletme(isletmeContext.getir())
                .ogrenci(ogrenci)
                .set(set)
                .setAd(set != null ? set.getAd() : null)
                .setTutari(BigDecimal.ONE)      // aşağıda kalemlerden hesaplanır
                .odenenTutar(BigDecimal.ZERO)
                .kullanici(mevcutKullanici.getir())
                .build();
        Map<EnvanterTur, BigDecimal> fiyatlar = new EnumMap<>(EnvanterTur.class);
        for (EnvanterTur tur : istekler.keySet()) {
            Envanter envanter = envanterler.get(tur);
            BigDecimal fiyat = fiyatBelirle(istekler.get(tur), envanter.getSatisFiyati());
            fiyatlar.put(tur, fiyat);
            satis.getKalemler().add(EnvanterSatisKalem.builder()
                    .satis(satis)
                    .envanter(envanter)
                    .birimFiyat(fiyat)
                    .odendi(istekler.get(tur).odendi())
                    .build());
        }
        BigDecimal toplam = toplam(fiyatlar);
        satis.setSetTutari(toplam);
        satis.setOdenenTutar(odenenTutarBelirle(request, istekler, fiyatlar, toplam));
        odemeleriEsitle(satis, request);
        satisRepository.save(satis);

        for (EnvanterSatisKalem kalem : satis.getKalemler()) {
            if (istekler.get(kalem.getEnvanter().getTur()).teslim()) {
                stokService.teslimEt(kalem.getEnvanter(), satis, "teslim edildi");
                kalem.setTeslimEdildi(true);
            }
        }
        return EnvanterSatisResponse.from(satis);
    }

    @Transactional
    public EnvanterSatisResponse guncelle(Long ogrenciId, Long satisId, EnvanterSatisRequest request) {
        Long isletmeId = isletmeContext.id();
        EnvanterSatis satis = satisGetir(ogrenciId, satisId, isletmeId);
        Map<EnvanterTur, EnvanterSatisRequest.Kalem> istekler = kalemleriTopla(request);
        Map<EnvanterTur, Envanter> hedefler = envanterleriBul(isletmeId, istekler);
        Map<EnvanterTur, EnvanterSatisKalem> mevcut = satis.getKalemler().stream()
                .collect(Collectors.toMap(k -> k.getEnvanter().getTur(), Function.identity()));

        // Önce tüm istek doğrulanır; hatalıysa hiçbir stok/kalem değişmez
        for (EnvanterTur tur : mevcut.keySet()) {
            EnvanterSatisKalem kalem = mevcut.get(tur);
            if (istekler.containsKey(tur) && kalem.isTeslimEdildi()
                    && !kalem.getEnvanter().getId().equals(hedefler.get(tur).getId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Teslim edilen envanterin bedeni değiştirilemez: " + EnvanterStokService.ad(kalem.getEnvanter())
                                + " (önce teslimi geri alıp kaydedin)");
            }
        }

        // Satıştan çıkarılan envanter: teslim edilmişse stoğu geri döner
        for (EnvanterTur tur : mevcut.keySet()) {
            if (!istekler.containsKey(tur)) {
                EnvanterSatisKalem kalem = mevcut.get(tur);
                if (kalem.isTeslimEdildi()) {
                    stokService.iade(kalem.getEnvanter(), satis, "satıştan çıkarıldı");
                }
                satis.getKalemler().remove(kalem);
            }
        }

        for (EnvanterTur tur : istekler.keySet()) {
            EnvanterSatisRequest.Kalem istek = istekler.get(tur);
            Envanter hedef = hedefler.get(tur);
            EnvanterSatisKalem kalem = mevcut.get(tur);

            if (kalem == null) {   // satışa sonradan eklenen envanter
                kalem = EnvanterSatisKalem.builder()
                        .satis(satis)
                        .envanter(hedef)
                        .birimFiyat(fiyatBelirle(istek, hedef.getSatisFiyati()))
                        .odendi(istek.odendi())
                        .build();
                satis.getKalemler().add(kalem);
                if (istek.teslim()) {
                    stokService.teslimEt(hedef, satis, "teslim edildi");
                    kalem.setTeslimEdildi(true);
                }
                continue;
            }
            if (kalem.isTeslimEdildi()) {
                if (!istek.teslim()) {
                    stokService.iade(kalem.getEnvanter(), satis, "teslim geri alındı");
                    kalem.setTeslimEdildi(false);
                }
            } else {
                if (!kalem.getEnvanter().getId().equals(hedef.getId())) {
                    kalem.setEnvanter(hedef);
                    kalem.setBirimFiyat(hedef.getSatisFiyati());     // beden değişince yeni bedenin güncel fiyatı
                }
                if (istek.teslim()) {
                    stokService.teslimEt(hedef, satis, "teslim edildi");
                    kalem.setTeslimEdildi(true);
                }
            }
            kalem.setOdendi(istek.odendi());
            if (istek.birimFiyat() != null) {
                kalem.setBirimFiyat(fiyatBelirle(istek, kalem.getBirimFiyat()));   // elle girilen fiyat geçerli olur
            }
        }

        Map<EnvanterTur, BigDecimal> fiyatlar = new EnumMap<>(EnvanterTur.class);
        satis.getKalemler().forEach(kalem -> fiyatlar.put(kalem.getEnvanter().getTur(), kalem.getBirimFiyat()));
        BigDecimal set = toplam(fiyatlar);
        satis.setSetTutari(set);
        satis.setOdenenTutar(odenenTutarBelirle(request, istekler, fiyatlar, set));
        odemeleriEsitle(satis, request);
        satis.setKullanici(mevcutKullanici.getir());
        satisRepository.flush();   // yeni ödeme satırlarının id'si yanıtta dolu gelsin
        return EnvanterSatisResponse.from(satis);
    }

    /**
     * Ödeme satırlarını satışın toplam ödenen tutarına eşitler. Toplam artmışsa fark, istekteki yöntemle yeni bir ödeme olarak
     * bugünün tarihine yazılır (yöntem zorunlu; Havale / EFT'de hesap da); azalmışsa en son ödemelerden düşülür.
     */
    private void odemeleriEsitle(EnvanterSatis satis, EnvanterSatisRequest request) {
        BigDecimal mevcut = satis.getOdemeler().stream().map(EnvanterSatisOdeme::getTutar).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal hedef = satis.getOdenenTutar();
        if (satis.getId() != null && hedef.compareTo(iadeRepository.envanterSatisToplami(satis.getId())) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bu satış için iade kaydı var; ödenen tutar iade tutarının altına düşürülemez");
        }
        int fark = hedef.compareTo(mevcut);
        if (fark > 0) {
            if (request.odemeSekli() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Ödeme şekli seçilmelidir");
            }
            satis.getOdemeler().add(EnvanterSatisOdeme.builder()
                    .satis(satis)
                    .tutar(hedef.subtract(mevcut))
                    .odemeSekli(request.odemeSekli())
                    .hesap(hesapService.coz(request.hesapId(), request.odemeSekli(), null))
                    .odemeTarihi(AppZaman.simdi())
                    .kullanici(mevcutKullanici.getir())
                    .build());
        } else if (fark < 0) {
            BigDecimal dusulecek = mevcut.subtract(hedef);
            for (int i = satis.getOdemeler().size() - 1; i >= 0 && dusulecek.signum() > 0; i--) {
                EnvanterSatisOdeme odeme = satis.getOdemeler().get(i);
                if (odeme.getTutar().compareTo(dusulecek) <= 0) {
                    dusulecek = dusulecek.subtract(odeme.getTutar());
                    satis.getOdemeler().remove(i);
                } else {
                    odeme.setTutar(odeme.getTutar().subtract(dusulecek));
                    dusulecek = BigDecimal.ZERO;
                }
            }
        }
    }

    /** Yanlış girilmiş bir ödemeyi siler. Satışın en az bir ödemesi kalmalıdır. */
    @Transactional
    public EnvanterSatisResponse odemeSil(Long ogrenciId, Long satisId, Long odemeId) {
        EnvanterSatis satis = satisGetir(ogrenciId, satisId, isletmeContext.id());
        EnvanterSatisOdeme odeme = satis.getOdemeler().stream().filter(o -> o.getId().equals(odemeId)).findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ödeme bulunamadı: " + odemeId));
        if (satis.getOdemeler().size() == 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Satışın en az bir ödemesi olmalıdır; ödemeyi silmek yerine satışı silin");
        }
        BigDecimal kalanOdeme = satis.getOdenenTutar().subtract(odeme.getTutar());
        if (kalanOdeme.compareTo(iadeRepository.envanterSatisToplami(satis.getId())) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bu satış için iade kaydı var; ödeme silinirse ödenen tutar iade tutarının altına düşer");
        }
        satis.getOdemeler().remove(odeme);
        satis.setOdenenTutar(kalanOdeme);
        satis.setKullanici(mevcutKullanici.getir());
        return EnvanterSatisResponse.from(satis);
    }

    /** Satışı siler; teslim edilmiş kalemlerin stoğu geri döner. */
    @Transactional
    public void sil(Long ogrenciId, Long satisId) {
        EnvanterSatis satis = satisGetir(ogrenciId, satisId, isletmeContext.id());
        if (iadeRepository.envanterSatisToplami(satisId).signum() > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "İadesi yapılmış satış silinemez");
        }
        for (EnvanterSatisKalem kalem : satis.getKalemler()) {
            if (kalem.isTeslimEdildi()) {
                stokService.iade(kalem.getEnvanter(), satis, "satış silindi");
            }
        }
        satisRepository.delete(satis);
    }

    /** Set seçildiyse işletmeye ait olmalı ve satıştaki türler setin türleriyle aynı olmalı (beden değiştirilebilir). */
    private EnvanterSet setBul(Long isletmeId, Long setId, Map<EnvanterTur, EnvanterSatisRequest.Kalem> istekler) {
        if (setId == null) {
            return null;
        }
        EnvanterSet set = setRepository.bul(setId, isletmeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Set bulunamadı: " + setId));
        Set<EnvanterTur> setTurleri = set.getEnvanterler().stream().map(Envanter::getTur)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(EnvanterTur.class)));
        if (!setTurleri.equals(istekler.keySet())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Satıştaki envanterler \"" + set.getAd() + "\" setiyle uyuşmuyor");
        }
        return set;
    }

    /** En az bir kalem gelmeli; her envanter türünden en fazla bir tane. */
    private Map<EnvanterTur, EnvanterSatisRequest.Kalem> kalemleriTopla(EnvanterSatisRequest request) {
        if (request.kalemler().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Satış için en az bir envanter seçilmelidir");
        }
        Map<EnvanterTur, EnvanterSatisRequest.Kalem> sonuc = new EnumMap<>(EnvanterTur.class);
        for (EnvanterSatisRequest.Kalem kalem : request.kalemler()) {
            if (sonuc.put(kalem.tur(), kalem) != null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Aynı envanter birden fazla kez gönderildi: " + kalem.tur().getAd());
            }
        }
        return sonuc;
    }

    private Map<EnvanterTur, Envanter> envanterleriBul(Long isletmeId, Map<EnvanterTur, EnvanterSatisRequest.Kalem> istekler) {
        Map<EnvanterTur, Envanter> sonuc = new EnumMap<>(EnvanterTur.class);
        for (EnvanterTur tur : istekler.keySet()) {
            String beden = EnvanterBeden.STANDART;
            if (tur.isBedenli()) {
                beden = istekler.get(tur).beden();
                if (!EnvanterBeden.gecerli(beden)) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, tur.getAd() + " için beden seçilmelidir");
                }
            }
            String aranan = beden;
            sonuc.put(tur, envanterRepository.findByIsletmeIdAndTurAndBeden(isletmeId, tur, aranan)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                            tur.getAd() + " · " + aranan + " için envanter kaydı yok; önce Envanter menüsünden ekleyin")));
        }
        return sonuc;
    }

    /** İstekte fiyat varsa o, yoksa varsayılan (envanter fiyatı ya da kalemin mevcut fiyatı). */
    private BigDecimal fiyatBelirle(EnvanterSatisRequest.Kalem istek, BigDecimal varsayilan) {
        return istek.birimFiyat() != null ? istek.birimFiyat().setScale(2, RoundingMode.HALF_UP) : varsayilan;
    }

    private BigDecimal toplam(Map<EnvanterTur, BigDecimal> fiyatlar) {
        return fiyatlar.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    /** Tutar gönderilmediyse Ödendi işaretli kalemlerin toplamı; 0'dan büyük olmalı ve set tutarını aşamaz. */
    private BigDecimal odenenTutarBelirle(EnvanterSatisRequest request, Map<EnvanterTur, EnvanterSatisRequest.Kalem> istekler,
                                          Map<EnvanterTur, BigDecimal> fiyatlar, BigDecimal set) {
        BigDecimal odenen;
        if (request.odenenTutar() != null) {
            odenen = request.odenenTutar().setScale(2, RoundingMode.HALF_UP);
        } else {
            odenen = istekler.entrySet().stream()
                    .filter(e -> e.getValue().odendi())
                    .map(e -> fiyatlar.get(e.getKey()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
        }
        if (odenen.signum() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ödenen tutar girilmelidir");
        }
        if (odenen.compareTo(set) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ödenen tutar set tutarını (" + set + ") aşamaz");
        }
        return odenen;
    }

    private Ogrenci ogrenciGetir(Long ogrenciId, Long isletmeId) {
        return ogrenciRepository.findByIdAndIsletmeId(ogrenciId, isletmeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Öğrenci bulunamadı: " + ogrenciId));
    }

    private EnvanterSatis satisGetir(Long ogrenciId, Long satisId, Long isletmeId) {
        return satisRepository.bul(satisId, isletmeId, ogrenciId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Envanter satışı bulunamadı: " + satisId));
    }
}
