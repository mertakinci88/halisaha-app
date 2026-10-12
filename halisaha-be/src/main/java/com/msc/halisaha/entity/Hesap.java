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

import java.time.LocalDateTime;

/** İşletmenin Havale / EFT ile para alabildiği banka hesabı. */
@Entity
@Table(name = "hesap")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hesap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "isletme_id", nullable = false)
    private Isletme isletme;

    @Column(name = "ad", nullable = false)
    private String ad;

    /** Boşluksuz, büyük harfli (TR + 24 hane). */
    @Column(name = "iban", nullable = false, length = 26)
    private String iban;

    @Column(name = "hesap_sahibi", nullable = false)
    private String hesapSahibi;

    @Enumerated(EnumType.STRING)
    @Column(name = "durum", nullable = false, length = 20)
    private HesapDurum durum;

    @Column(name = "kayit_tarih", nullable = false, updatable = false)
    private LocalDateTime kayitTarih;

    @PrePersist
    void prePersist() {
        if (kayitTarih == null) {
            kayitTarih = LocalDateTime.now();
        }
        if (durum == null) {
            durum = HesapDurum.AKTIF;
        }
    }
}
