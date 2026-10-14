package com.msc.halisaha.entity;

import com.msc.halisaha.common.AppZaman;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
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
import java.util.ArrayList;
import java.util.List;

/** Bir sahanın haftanın belli günü ve saatinde, başlangıç ile bitiş tarihi arasında (1 yıl) abone müşteriye ayrılması. */
@Entity
@Table(name = "abonelik")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Abonelik {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "isletme_id", nullable = false)
    private Isletme isletme;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "saha_id", nullable = false)
    private Saha saha;

    @Column(name = "ad_soyad", nullable = false)
    private String adSoyad;

    @Column(name = "tel_no", nullable = false, length = 11)
    private String telNo;

    /** 1 = Pazartesi ... 7 = Pazar. */
    @Column(name = "hafta_gunu", nullable = false)
    private int haftaGunu;

    /** 12-25 (24 = 00:00, 25 = 01:00). */
    @Column(name = "baslangic_saat", nullable = false)
    private int baslangicSaat;

    @Column(name = "sure_saat", nullable = false)
    private int sureSaat;

    @Column(name = "baslangic_tarih", nullable = false)
    private LocalDate baslangicTarih;

    /** Başlangıç tarihinden 1 yıl sonrası; kayıtta otomatik hesaplanır. */
    @Column(name = "bitis_tarih", nullable = false)
    private LocalDate bitisTarih;

    @Enumerated(EnumType.STRING)
    @Column(name = "durum", nullable = false, length = 20)
    private AbonelikDurum durum;

    /** Abonelik için anlaşılan toplam tutar. */
    @Column(name = "ucret", nullable = false, precision = 10, scale = 2)
    private BigDecimal ucret;

    @Column(name = "iptal_tarih")
    private LocalDateTime iptalTarih;

    @Column(name = "\"not\"", length = 255)
    private String not;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kullanici_id")
    private Kullanici kullanici;

    @Column(name = "kayit_tarih", nullable = false, updatable = false)
    private LocalDateTime kayitTarih;

    @OneToMany(mappedBy = "abonelik", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("odemeTarihi, id")
    @Builder.Default
    private List<AbonelikOdeme> odemeler = new ArrayList<>();

    @PrePersist
    void prePersist() {
        kayitTarih = AppZaman.simdi();
        if (durum == null) {
            durum = AbonelikDurum.AKTIF;
        }
    }
}
