package com.msc.halisaha.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ogrenci")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ogrenci {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "isletme_id", nullable = false)
    private Isletme isletme;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grup_id")
    private Grup grup;

    @Column(name = "ad_soyad", nullable = false)
    private String adSoyad;

    @Column(name = "dogum_tarih")
    private LocalDate dogumTarih;

    @Column(name = "veli_ad_soyad")
    private String veliAdSoyad;

    @Column(name = "veli_tel_no")
    private String veliTelNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "durum", nullable = false, length = 20)
    private OgrenciDurum durum;

    @Column(name = "antrenman_gun_sayisi")
    private Integer antrenmanGunSayisi;

    /** Kapalıysa öğrenciden ödeme alınmaz: ödeme durumu, borçlu listesi ve ödeme raporlarında yer almaz. */
    @Column(name = "odeme_alinir", nullable = false)
    @Builder.Default
    private boolean odemeAlinir = true;

    @Column(name = "kayit_tarih", nullable = false, updatable = false)
    private LocalDateTime kayitTarih;

    @Column(name = "\"not\"")
    private String not;

    @PrePersist
    void prePersist() {
        if (durum == null) {
            durum = OgrenciDurum.AKTIF;
        }
        if (kayitTarih == null) {
            kayitTarih = LocalDateTime.now();
        }
    }
}
