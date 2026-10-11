package com.msc.halisaha.entity;

import com.msc.halisaha.common.AppZaman;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Bir öğrencinin bir günkü yoklaması. Kaydı olmayan öğrenci o gün için işaretlenmemiştir. */
@Entity
@Table(name = "yoklama")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Yoklama {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "isletme_id", nullable = false)
    private Isletme isletme;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ogrenci_id", nullable = false)
    private Ogrenci ogrenci;

    @Column(name = "tarih", nullable = false)
    private LocalDate tarih;

    @Enumerated(EnumType.STRING)
    @Column(name = "durum", nullable = false, length = 20)
    private YoklamaDurum durum;

    @Column(name = "aciklama")
    private String aciklama;

    /** Yoklamayı son kaydeden kullanıcı. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kullanici_id")
    private Kullanici kullanici;

    @Column(name = "olusturma_tarihi", nullable = false, updatable = false)
    private LocalDateTime olusturmaTarihi;

    @Column(name = "guncelleme_tarihi", nullable = false)
    private LocalDateTime guncellemeTarihi;

    @PrePersist
    void prePersist() {
        LocalDateTime simdi = AppZaman.simdi();
        olusturmaTarihi = simdi;
        guncellemeTarihi = simdi;
    }

    @PreUpdate
    void preUpdate() {
        guncellemeTarihi = AppZaman.simdi();
    }
}
