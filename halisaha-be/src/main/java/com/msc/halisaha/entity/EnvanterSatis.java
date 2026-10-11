package com.msc.halisaha.entity;

import com.msc.halisaha.common.AppZaman;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Bir öğrenciye satılan envanter seti (her envanter türünden bir kalem). */
@Entity
@Table(name = "envanter_satis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnvanterSatis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "isletme_id", nullable = false)
    private Isletme isletme;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ogrenci_id", nullable = false)
    private Ogrenci ogrenci;

    /** Satışın başlatıldığı set (set silinirse boşalır); özel satışta boş. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "set_id")
    private EnvanterSet set;

    /** Satış anındaki set adı; set sonradan silinse / adı değişse de görünür. */
    @Column(name = "set_ad", length = 100)
    private String setAd;

    /** Kalemlerin birim fiyat toplamı. */
    @Column(name = "set_tutari", nullable = false, precision = 10, scale = 2)
    private BigDecimal setTutari;

    @Column(name = "odenen_tutar", nullable = false, precision = 10, scale = 2)
    private BigDecimal odenenTutar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kullanici_id")
    private Kullanici kullanici;

    @Column(name = "kayit_tarihi", nullable = false, updatable = false)
    private LocalDateTime kayitTarihi;

    @Column(name = "guncelleme_tarihi", nullable = false)
    private LocalDateTime guncellemeTarihi;

    @OneToMany(mappedBy = "satis", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EnvanterSatisKalem> kalemler = new ArrayList<>();

    @PrePersist
    void prePersist() {
        LocalDateTime simdi = AppZaman.simdi();
        kayitTarihi = simdi;
        guncellemeTarihi = simdi;
    }

    @PreUpdate
    void preUpdate() {
        guncellemeTarihi = AppZaman.simdi();
    }
}
