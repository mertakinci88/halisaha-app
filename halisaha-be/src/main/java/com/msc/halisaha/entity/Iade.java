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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Alınan bir ödemenin geri verilmesi; rezervasyon, öğrenci aylık ödemesi, envanter satışı ya da abonelikten tam olarak birine bağlıdır. */
@Entity
@Table(name = "iade")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Iade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "isletme_id", nullable = false)
    private Isletme isletme;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rezervasyon_id")
    private Rezervasyon rezervasyon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ogrenci_odeme_id")
    private OgrenciOdeme ogrenciOdeme;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "envanter_satis_id")
    private EnvanterSatis envanterSatis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "abonelik_id")
    private Abonelik abonelik;

    @Column(name = "tutar", nullable = false, precision = 10, scale = 2)
    private BigDecimal tutar;

    @Enumerated(EnumType.STRING)
    @Column(name = "iade_sekli", nullable = false, length = 20)
    private OdemeYontemi iadeSekli;

    /** Havale / EFT iadede paranın çıktığı hesap (diğer yöntemlerde null). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hesap_id")
    private Hesap hesap;

    @Column(name = "iade_tarihi", nullable = false)
    private LocalDate iadeTarihi;

    @Column(name = "neden", length = 255)
    private String neden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kullanici_id")
    private Kullanici kullanici;

    @Column(name = "kayit_tarih", nullable = false, updatable = false)
    private LocalDateTime kayitTarih;
}
