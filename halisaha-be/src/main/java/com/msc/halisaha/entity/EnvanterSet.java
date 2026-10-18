package com.msc.halisaha.entity;

import com.msc.halisaha.common.AppZaman;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
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

/** İşletmenin tanımladığı envanter seti: adı, bedeni, tutarı ve envanter kayıtları (set bedeninden seçilen bedenli ürünler + seçilen bedensiz ürünler). */
@Entity
@Table(name = "envanter_set")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnvanterSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "isletme_id", nullable = false)
    private Isletme isletme;

    @Column(name = "ad", nullable = false, length = 100)
    private String ad;

    /** Setin bedeni: sete seçilen bedenli envanterler bu bedenden olur. */
    @Column(name = "beden", nullable = false, length = 10)
    private String beden;

    /** Setin satış tutarı; kalemlerin tek tek fiyatı yoktur. */
    @Column(name = "tutar", nullable = false, precision = 10, scale = 2)
    private BigDecimal tutar;

    /** Seti son kaydeden kullanıcı. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kullanici_id")
    private Kullanici kullanici;

    @Column(name = "kayit_tarihi", nullable = false, updatable = false)
    private LocalDateTime kayitTarihi;

    @Column(name = "guncelleme_tarihi", nullable = false)
    private LocalDateTime guncellemeTarihi;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "envanter_set_kalem",
            joinColumns = @JoinColumn(name = "set_id"),
            inverseJoinColumns = @JoinColumn(name = "envanter_id"))
    @Builder.Default
    private List<Envanter> envanterler = new ArrayList<>();

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
