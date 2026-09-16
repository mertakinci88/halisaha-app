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
import java.time.LocalDateTime;

@Entity
@Table(name = "rezervasyon")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rezervasyon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "saha_id", nullable = false)
    private Saha saha;

    @Column(name = "ad_soyad", nullable = false)
    private String adSoyad;

    @Column(name = "baslangic_tarih", nullable = false)
    private LocalDateTime baslangicTarih;

    @Column(name = "bitis_tarih", nullable = false)
    private LocalDateTime bitisTarih;

    @Enumerated(EnumType.STRING)
    @Column(name = "durum", nullable = false, length = 20)
    private RezervasyonDurum durum;

    @Column(name = "tel_no", nullable = false)
    private String telNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "odeme_yontemi", length = 20)
    private OdemeYontemi odemeYontemi;

    @Enumerated(EnumType.STRING)
    @Column(name = "odeme_durumu", nullable = false, length = 20)
    private OdemeDurumu odemeDurumu;

    @Column(name = "odenecek_tutar", nullable = false, precision = 10, scale = 2)
    private BigDecimal odenecekTutar;

    @PrePersist
    void prePersist() {
        if (durum == null) {
            durum = RezervasyonDurum.AKTIF;
        }
        if (odemeDurumu == null) {
            odemeDurumu = OdemeDurumu.ODENMEDI;
        }
    }
}
