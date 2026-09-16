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

import java.math.BigDecimal;
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

    @Column(name = "aylik_aidat", precision = 10, scale = 2)
    private BigDecimal aylikAidat;

    @Enumerated(EnumType.STRING)
    @Column(name = "odeme_durumu", nullable = false, length = 20)
    private OdemeDurumu odemeDurumu;

    @Column(name = "odeme_plani")
    private String odemePlani;

    @Column(name = "odenen_donem")
    private String odenenDonem;

    @Column(name = "odeme_tarihi")
    private LocalDate odemeTarihi;

    @Column(name = "sonraki_odeme_tarih")
    private LocalDate sonrakiOdemeTarih;

    @Column(name = "odenen_tutar", precision = 10, scale = 2)
    private BigDecimal odenenTutar;

    @Enumerated(EnumType.STRING)
    @Column(name = "odeme_sekli", length = 20)
    private OdemeYontemi odemeSekli;

    @Column(name = "kayit_tarih", nullable = false, updatable = false)
    private LocalDateTime kayitTarih;

    @Column(name = "\"not\"")
    private String not;

    @PrePersist
    void prePersist() {
        if (durum == null) {
            durum = OgrenciDurum.AKTIF;
        }
        if (odemeDurumu == null) {
            odemeDurumu = OdemeDurumu.ODENMEDI;
        }
        if (kayitTarih == null) {
            kayitTarih = LocalDateTime.now();
        }
    }
}
