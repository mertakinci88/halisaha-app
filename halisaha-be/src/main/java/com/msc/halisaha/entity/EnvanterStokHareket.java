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

@Entity
@Table(name = "envanter_stok_hareket")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnvanterStokHareket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "envanter_id", nullable = false)
    private Long envanterId;

    /** İlişki değil düz id: satış silinince DB bu alanı null yapar, hareket kalır. */
    @Column(name = "satis_id")
    private Long satisId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tip", nullable = false, length = 10)
    private EnvanterStokHareketTip tip;

    @Column(name = "degisim", nullable = false)
    private Integer degisim;

    @Column(name = "kalan_adet", nullable = false)
    private Integer kalanAdet;

    @Column(name = "aciklama")
    private String aciklama;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kullanici_id")
    private Kullanici kullanici;

    @Column(name = "olusma_tarihi", nullable = false)
    private LocalDateTime olusmaTarihi;

    @PrePersist
    void prePersist() {
        if (olusmaTarihi == null) {
            olusmaTarihi = AppZaman.simdi();
        }
    }
}
