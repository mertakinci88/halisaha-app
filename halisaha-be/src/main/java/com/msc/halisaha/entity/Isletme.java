package com.msc.halisaha.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Uygulamayı kullanan bir işletme (kiracı). Saha, ürün, öğrenci vb. tüm veri bir işletmeye aittir. */
@Entity
@Table(name = "isletme")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Isletme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ad", nullable = false)
    private String ad;

    @Column(name = "yonetici_ad_soyad")
    private String yoneticiAdSoyad;

    @Column(name = "yonetici_tel_no")
    private String yoneticiTelNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "durum", nullable = false, length = 20)
    private IsletmeDurum durum;

    @Column(name = "kayit_tarih", nullable = false, updatable = false)
    private LocalDateTime kayitTarih;

    @PrePersist
    void prePersist() {
        if (durum == null) {
            durum = IsletmeDurum.AKTIF;
        }
        if (kayitTarih == null) {
            kayitTarih = LocalDateTime.now();
        }
    }
}
