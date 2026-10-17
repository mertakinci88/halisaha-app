package com.msc.halisaha.service;

import com.msc.halisaha.TestOturum;
import com.msc.halisaha.dto.EnvanterRequest;
import com.msc.halisaha.dto.EnvanterResponse;
import com.msc.halisaha.dto.EnvanterSatisRequest;
import com.msc.halisaha.entity.EnvanterTur;
import com.msc.halisaha.entity.Isletme;
import com.msc.halisaha.entity.Kullanici;
import com.msc.halisaha.entity.Ogrenci;
import com.msc.halisaha.entity.OgrenciDurum;
import com.msc.halisaha.entity.Rol;
import com.msc.halisaha.repository.IsletmeRepository;
import com.msc.halisaha.repository.KullaniciRepository;
import com.msc.halisaha.repository.OgrenciRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Envanter testleri için ortak kurulum: iki işletme, bir öğrenci ve her envanter türünden stok kaydı. */
final class EnvanterTestKurulum {

    Kullanici a;
    Kullanici b;
    Ogrenci ogrenciA;
    EnvanterResponse esofman140;   // 5 adet, 600
    EnvanterResponse esofmanMedium; // 0 adet (stoksuz), 600
    EnvanterResponse krampon;      // 22 adet, 350
    EnvanterResponse tozluk;       // 40 adet, 150
    EnvanterResponse yagmurluk164; // 4 adet, 500
    EnvanterResponse yagmurlukSmall; // 11 adet, 500
    EnvanterResponse forma128;     // 25 adet, 450

    EnvanterTestKurulum(IsletmeRepository isletmeRepository, KullaniciRepository kullaniciRepository,
                        OgrenciRepository ogrenciRepository, EnvanterService envanterService) {
        Isletme isletmeA = TestOturum.isletme(isletmeRepository, "A İşletmesi");
        Isletme isletmeB = TestOturum.isletme(isletmeRepository, "B İşletmesi");
        a = TestOturum.kullanici(kullaniciRepository, isletmeA, Rol.ADMIN);
        b = TestOturum.kullanici(kullaniciRepository, isletmeB, Rol.ADMIN);
        ogrenciA = ogrenciRepository.save(Ogrenci.builder().isletme(isletmeA).adSoyad("Ahmet Test").durum(OgrenciDurum.AKTIF).build());
        TestOturum.girisYap(a);
        esofman140 = envanterService.create(istek(EnvanterTur.KISLIK_ESOFMAN, "140", 5, "600"));
        esofmanMedium = envanterService.create(istek(EnvanterTur.KISLIK_ESOFMAN, "Medium", 0, "600"));
        krampon = envanterService.create(istek(EnvanterTur.KRAMPON_CANTASI, null, 22, "350"));
        tozluk = envanterService.create(istek(EnvanterTur.TOZLUK, null, 40, "150"));
        yagmurluk164 = envanterService.create(istek(EnvanterTur.YAGMURLUK, "164", 4, "500"));
        yagmurlukSmall = envanterService.create(istek(EnvanterTur.YAGMURLUK, "Small", 11, "500"));
        forma128 = envanterService.create(istek(EnvanterTur.YAZLIK_FORMA, "128", 25, "450"));
    }

    static EnvanterRequest istek(EnvanterTur tur, String beden, int adet, String fiyat) {
        return new EnvanterRequest(tur, beden, adet, new BigDecimal(fiyat), 5);
    }

    /** Set isteği: eşofman 140, krampon, tozluk, yağmurluk 164, forma 128; teslim/ödendi tur adlarıyla verilir. */
    static EnvanterSatisRequest set(String esofmanBeden, String yagmurlukBeden, List<EnvanterTur> teslim, List<EnvanterTur> odendi, String odenen) {
        List<EnvanterSatisRequest.Kalem> k = new ArrayList<>();
        for (EnvanterTur tur : EnvanterTur.values()) {
            if (tur == EnvanterTur.KALECI_FORMA) continue; // kaleci forması set testlerinin dışında
            String beden = switch (tur) {
                case KISLIK_ESOFMAN -> esofmanBeden;
                case YAGMURLUK -> yagmurlukBeden;
                case YAZLIK_FORMA -> "128";
                default -> null;
            };
            k.add(new EnvanterSatisRequest.Kalem(tur, beden, odendi.contains(tur), teslim.contains(tur)));
        }
        return new EnvanterSatisRequest(k, odenen != null ? new BigDecimal(odenen) : null);
    }
}
