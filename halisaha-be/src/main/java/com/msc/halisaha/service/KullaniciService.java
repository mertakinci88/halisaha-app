package com.msc.halisaha.service;

import com.msc.halisaha.common.TurkceSirala;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.common.security.IsletmeContext;
import com.msc.halisaha.dto.GrupYetkiGuncelleRequest;
import com.msc.halisaha.dto.KullaniciGuncelleRequest;
import com.msc.halisaha.dto.KullaniciOlusturRequest;
import com.msc.halisaha.dto.KullaniciResponse;
import com.msc.halisaha.dto.RolGuncelleRequest;
import com.msc.halisaha.dto.YetkiGuncelleRequest;
import com.msc.halisaha.entity.Isletme;
import com.msc.halisaha.entity.Kullanici;
import com.msc.halisaha.entity.KullaniciDurum;
import com.msc.halisaha.entity.Modul;
import com.msc.halisaha.entity.Rol;
import com.msc.halisaha.repository.GrupRepository;
import com.msc.halisaha.repository.IsletmeRepository;
import com.msc.halisaha.repository.KullaniciRepository;
import com.msc.halisaha.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Kullanıcı yönetimi her zaman tek bir işletme kapsamındadır. ADMIN kendi işletmesini
 * ({@code ...Mevcut} metodları), SUPER_ADMIN ise path'ten verdiği işletmeyi yönetir.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KullaniciService {

    private final KullaniciRepository kullaniciRepository;
    private final IsletmeRepository isletmeRepository;
    private final PasswordEncoder passwordEncoder;
    private final IsletmeContext isletmeContext;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MevcutKullanici mevcutKullanici;
    private final GrupRepository grupRepository;

    public List<KullaniciResponse> list() {
        return list(isletmeContext.id());
    }

    public List<KullaniciResponse> list(Long isletmeId) {
        // Sıra sabit ve alfabetik olmalı: sıralamasız sorguda Postgres, güncellenen (ör. durumu değişen) satırı farklı yerde döndürebilir
        return kullaniciRepository.findByIsletmeId(isletmeId).stream()
                .sorted(TurkceSirala.<Kullanici>alfabetik(Kullanici::getAdSoyad).thenComparing(Kullanici::getId))
                .map(KullaniciResponse::from)
                .toList();
    }

    public KullaniciResponse getById(Long id) {
        return KullaniciResponse.from(getEntity(isletmeContext.id(), id));
    }

    @Transactional
    public KullaniciResponse create(KullaniciOlusturRequest request) {
        return create(isletmeContext.id(), request);
    }

    @Transactional
    public KullaniciResponse create(Long isletmeId, KullaniciOlusturRequest request) {
        if (request.rol() == Rol.SUPER_ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bu rolde kullanıcı oluşturulamaz");
        }
        if (kullaniciRepository.findByKullaniciAdi(request.kullaniciAdi()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Bu kullanıcı adı zaten kullanılıyor");
        }
        Isletme isletme = isletmeRepository.findById(isletmeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "İşletme bulunamadı: " + isletmeId));
        Kullanici kullanici = Kullanici.builder()
                .isletme(isletme)
                .kullaniciAdi(request.kullaniciAdi())
                .sifre(passwordEncoder.encode(request.sifre()))
                .adSoyad(request.adSoyad())
                .rol(request.rol())
                .durum(KullaniciDurum.AKTIF)
                .yetkiler(request.rol() == Rol.ADMIN || request.yetkiler() == null
                        ? new HashSet<>()
                        : yetkiTemizle(request.yetkiler()))
                .build();
        if (request.rol() == Rol.PERSONEL && Boolean.TRUE.equals(request.grupKisitli())) {
            grupKisitiUygula(kullanici, isletmeId, true, request.grupIdleri());
        }
        return KullaniciResponse.from(kullaniciRepository.save(kullanici));
    }

    /** Yalnızca oturumdaki kullanıcının kendi işletmesindeki bir kullanıcıyı günceller (başkasınınki 404). */
    @Transactional
    public KullaniciResponse guncelle(Long id, KullaniciGuncelleRequest request) {
        Kullanici kullanici = getEntity(isletmeContext.id(), id);
        kullanici.setAdSoyad(request.adSoyad().trim());

        String yeniKullaniciAdi = request.kullaniciAdi().trim();
        boolean kullaniciAdiDegisti = !yeniKullaniciAdi.equals(kullanici.getKullaniciAdi());
        if (kullaniciAdiDegisti) {
            // Kullanıcı adı tüm işletmeler genelinde benzersizdir
            if (kullaniciRepository.findByKullaniciAdi(yeniKullaniciAdi).isPresent()) {
                throw new ApiException(HttpStatus.CONFLICT, "Bu kullanıcı adı zaten kullanılıyor");
            }
            kullanici.setKullaniciAdi(yeniKullaniciAdi);
        }

        boolean sifreDegisti = request.sifre() != null && !request.sifre().isBlank();
        if (sifreDegisti) {
            kullanici.setSifre(passwordEncoder.encode(request.sifre()));
        }
        // Eski kullanıcı adıyla/şifreyle açılmış oturumlar yenilenemesin; kullanıcı yeniden giriş yapar
        if (kullaniciAdiDegisti || sifreDegisti) {
            refreshTokenRepository.tumunuIptalEt(kullanici.getId());
        }
        return KullaniciResponse.from(kullanici);
    }

    /**
     * Kullanıcının rolünü ADMIN ↔ PERSONEL arasında değiştirir. Admin kendi rolünü değiştiremez
     * (işletmede admin kalmaması engellenir). Arayüz kullanıcı/menü bilgisini oturumda tuttuğu için açık oturumlar kapatılır; kullanıcı yeniden giriş yapar.
     */
    @Transactional
    public KullaniciResponse rolGuncelle(Long id, RolGuncelleRequest request) {
        if (request.rol() == Rol.SUPER_ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bu role atama yapılamaz");
        }
        Kullanici kullanici = getEntity(isletmeContext.id(), id);
        Kullanici ben = mevcutKullanici.getir();
        if (ben != null && ben.getId().equals(kullanici.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Kendi rolünüzü değiştiremezsiniz");
        }
        if (kullanici.getRol() == request.rol()) {
            return KullaniciResponse.from(kullanici);
        }
        kullanici.setRol(request.rol());
        // Admin tüm modüllere erişir; eski menü yetkileri kalıntı olmasın. Personel yetkisiz başlar.
        kullanici.getYetkiler().clear();
        // Grup kısıtı yalnızca personelde anlamlıdır; rol değişince sıfırlanır (tüm gruplar)
        kullanici.setGrupKisitli(false);
        kullanici.getIzinliGruplar().clear();
        refreshTokenRepository.tumunuIptalEt(kullanici.getId());
        return KullaniciResponse.from(kullanici);
    }

    @Transactional
    public KullaniciResponse yetkileriGuncelle(Long id, YetkiGuncelleRequest request) {
        Kullanici kullanici = getEntity(isletmeContext.id(), id);
        if (kullanici.getRol() == Rol.ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Admin kullanıcısının yetkileri sınırlandırılamaz");
        }
        Set<Modul> yeniYetkiler = kullanici.getYetkiler();
        yeniYetkiler.clear();
        yeniYetkiler.addAll(yetkiTemizle(request.yetkiler()));
        return KullaniciResponse.from(kullanici);
    }

    /** Personelin hangi grupların öğrencilerini göreceği: kısıt yoksa tüm gruplar, varsa yalnızca seçilenler. */
    @Transactional
    public KullaniciResponse grupYetkisiniGuncelle(Long id, GrupYetkiGuncelleRequest request) {
        Kullanici kullanici = getEntity(isletmeContext.id(), id);
        if (kullanici.getRol() != Rol.PERSONEL) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Grup yetkisi yalnızca personel için tanımlanır");
        }
        grupKisitiUygula(kullanici, isletmeContext.id(), Boolean.TRUE.equals(request.grupKisitli()), request.grupIdleri());
        return KullaniciResponse.from(kullanici);
    }

    private void grupKisitiUygula(Kullanici kullanici, Long isletmeId, boolean kisitli, Set<Long> grupIdleri) {
        Set<Long> secilen = grupIdleri != null ? grupIdleri : Set.of();
        if (kisitli && secilen.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "En az bir grup seçmelisiniz");
        }
        for (Long grupId : secilen) {
            if (grupRepository.findByIdAndIsletmeId(grupId, isletmeId).isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Grup bulunamadı: " + grupId);
            }
        }
        kullanici.setGrupKisitli(kisitli);
        kullanici.getIzinliGruplar().clear();
        if (kisitli) {
            kullanici.getIzinliGruplar().addAll(secilen);
        }
    }

    @Transactional
    public KullaniciResponse durumGuncelle(Long id, KullaniciDurum durum) {
        return durumGuncelle(isletmeContext.id(), id, durum);
    }

    @Transactional
    public KullaniciResponse durumGuncelle(Long isletmeId, Long id, KullaniciDurum durum) {
        Kullanici kullanici = getEntity(isletmeId, id);
        boolean sonAktifAdmin = kullanici.getRol() == Rol.ADMIN
                && durum == KullaniciDurum.PASIF
                && kullaniciRepository.countByIsletmeIdAndRolAndDurum(isletmeId, Rol.ADMIN, KullaniciDurum.AKTIF) <= 1;
        if (sonAktifAdmin) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "İşletmedeki son aktif admin pasif hale getirilemez");
        }
        kullanici.setDurum(durum);
        return KullaniciResponse.from(kullanici);
    }

    @Transactional
    public void sifreSifirla(Long isletmeId, Long id, String yeniSifre) {
        sifreDegistir(getEntity(isletmeId, id), yeniSifre);
    }

    private void sifreDegistir(Kullanici kullanici, String yeniSifre) {
        kullanici.setSifre(passwordEncoder.encode(yeniSifre));
        refreshTokenRepository.tumunuIptalEt(kullanici.getId());
    }

    /** Öğrenci işlem yetkileri (transfer / ödeme / envanter / yoklama kartları) Öğrenciler menüsü olmadan anlamsızdır; menü yoksa düşürülür. */
    private static Set<Modul> yetkiTemizle(Set<Modul> yetkiler) {
        Set<Modul> sonuc = new HashSet<>(yetkiler);
        if (!sonuc.contains(Modul.OGRENCI)) {
            sonuc.removeAll(Set.of(Modul.OGRENCI_TRANSFER, Modul.OGRENCI_ODEME, Modul.OGRENCI_ENVANTER, Modul.OGRENCI_YOKLAMA));
        }
        return sonuc;
    }

    private Kullanici getEntity(Long isletmeId, Long id) {
        return kullaniciRepository.findByIdAndIsletmeId(id, isletmeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı: " + id));
    }
}
