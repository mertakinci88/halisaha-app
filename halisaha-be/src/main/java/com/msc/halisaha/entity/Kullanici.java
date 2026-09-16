package com.msc.halisaha.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "kullanici")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Kullanici {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kullanici_adi", nullable = false, unique = true)
    private String kullaniciAdi;

    @Column(name = "sifre", nullable = false)
    private String sifre;

    @Column(name = "ad_soyad", nullable = false)
    private String adSoyad;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    private Rol rol;

    @Enumerated(EnumType.STRING)
    @Column(name = "durum", nullable = false, length = 20)
    private KullaniciDurum durum;

    @Column(name = "kayit_tarih", nullable = false, updatable = false)
    private LocalDateTime kayitTarih;

    /**
     * PERSONEL rolündeki kullanıcının kullanabildiği ek menüler. ADMIN için bu koleksiyon
     * dikkate alınmaz; ADMIN her zaman tüm modüllere erişebilir (bkz. UserPrincipal).
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "kullanici_yetki", joinColumns = @JoinColumn(name = "kullanici_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "modul", length = 30)
    @Builder.Default
    private Set<Modul> yetkiler = new HashSet<>();

    @PrePersist
    void prePersist() {
        if (kayitTarih == null) {
            kayitTarih = LocalDateTime.now();
        }
        if (durum == null) {
            durum = KullaniciDurum.AKTIF;
        }
    }
}
