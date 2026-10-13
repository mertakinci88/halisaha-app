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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Öğrencinin bir gruptan diğerine transfer talebi; onaylananlar öğrencinin transfer geçmişidir. */
@Entity
@Table(name = "ogrenci_transfer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OgrenciTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "isletme_id", nullable = false)
    private Isletme isletme;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ogrenci_id", nullable = false)
    private Ogrenci ogrenci;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kaynak_grup_id")
    private Grup kaynakGrup;

    /** Grup silinse / adı değişse de geçmiş okunsun diye talep (onay) anındaki ad kopyalanır. */
    @Column(name = "kaynak_grup_ad")
    private String kaynakGrupAd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hedef_grup_id")
    private Grup hedefGrup;

    @Column(name = "hedef_grup_ad", nullable = false)
    private String hedefGrupAd;

    @Enumerated(EnumType.STRING)
    @Column(name = "durum", nullable = false, length = 20)
    private TransferDurum durum;

    @Column(name = "aciklama", length = 500)
    private String aciklama;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "talep_eden_id")
    private Kullanici talepEden;

    @Column(name = "talep_tarihi", nullable = false, updatable = false)
    private LocalDateTime talepTarihi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "karar_veren_id")
    private Kullanici kararVeren;

    @Column(name = "karar_tarihi")
    private LocalDateTime kararTarihi;

    /** Red nedeni ya da iptal notu. */
    @Column(name = "karar_notu", length = 500)
    private String kararNotu;

    @PrePersist
    void prePersist() {
        if (durum == null) {
            durum = TransferDurum.BEKLIYOR;
        }
        if (talepTarihi == null) {
            talepTarihi = AppZaman.simdi();
        }
    }
}
