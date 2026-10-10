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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rezervasyon_kalem")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RezervasyonKalem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rezervasyon_id", nullable = false)
    private Rezervasyon rezervasyon;

    @Enumerated(EnumType.STRING)
    @Column(name = "tip", nullable = false, length = 10)
    private KalemTip tip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "urun_id")
    private Urun urun;

    @Column(name = "aciklama", nullable = false)
    private String aciklama;

    /** SAHA için saat, URUN için adet. */
    @Column(name = "miktar", nullable = false, precision = 8, scale = 2)
    private BigDecimal miktar;

    /** Kalem oluşturulduğu andaki birim fiyat; sonradan değişmez. */
    @Column(name = "birim_fiyat", nullable = false, precision = 10, scale = 2)
    private BigDecimal birimFiyat;

    @Column(name = "tutar", nullable = false, precision = 10, scale = 2)
    private BigDecimal tutar;

    /** Ürün kalemi için stoktan düşüldü mü; iade sadece düşülmüş kalemlerde yapılır. */
    @Column(name = "stok_dusuldu", nullable = false)
    private boolean stokDusuldu;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @PrePersist
    void prePersist() {
        if (olusturmaTarihi == null) {
            olusturmaTarihi = AppZaman.simdi();
        }
    }
}
