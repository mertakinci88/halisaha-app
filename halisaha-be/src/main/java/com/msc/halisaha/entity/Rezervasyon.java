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
    @JoinColumn(name = "isletme_id", nullable = false)
    private Isletme isletme;

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

    @Column(name = "tel_no", nullable = false, length = 11)
    private String telNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "odeme_yontemi", length = 20)
    private OdemeYontemi odemeYontemi;

    @Enumerated(EnumType.STRING)
    @Column(name = "odeme_durumu", nullable = false, length = 20)
    private OdemeDurumu odemeDurumu;

    /** Rezervasyonu sisteme giren kullanıcı; bu alan eklenmeden önceki kayıtlarda null. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kullanici_id")
    private Kullanici kullanici;

    /** Rezervasyonun sisteme girildiği an; bu alan eklenmeden önceki kayıtlarda null. */
    @Column(name = "kayit_tarih", updatable = false)
    private LocalDateTime kayitTarih;

    @Column(name = "odenecek_tutar", nullable = false, precision = 10, scale = 2)
    private BigDecimal odenecekTutar;

    /** İptal edilme zamanı, iptal eden kullanıcı ve (varsa) neden; bu alanlar eklenmeden önce iptal edilenlerde boştur. */
    @Column(name = "iptal_tarih")
    private LocalDateTime iptalTarih;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iptal_eden_id")
    private Kullanici iptalEden;

    @Column(name = "iptal_neden", length = 255)
    private String iptalNeden;

    /** Bu rezervasyon bir aboneliğin haftalık rezervasyonuysa o abonelik; normal rezervasyonda null. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "abonelik_id")
    private Abonelik abonelik;

    /** Antrenman aboneliğinin haftalık rezervasyonu: takvimde dolu görünür, ücreti ve ödemesi yoktur, raporlara girmez. */
    @Column(name = "antrenman", nullable = false)
    private boolean antrenman;

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
