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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Envanter satışı için alınan tek bir ödeme (kendi tarihi, tutarı ve yöntemiyle). */
@Entity
@Table(name = "envanter_satis_odeme")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnvanterSatisOdeme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "satis_id", nullable = false)
    private EnvanterSatis satis;

    @Column(name = "tutar", nullable = false, precision = 10, scale = 2)
    private BigDecimal tutar;

    /** Bu alan eklenmeden önceki satışların ödemelerinde boştur. */
    @Enumerated(EnumType.STRING)
    @Column(name = "odeme_sekli", length = 20)
    private OdemeYontemi odemeSekli;

    /** Havale / EFT ödemede paranın yattığı hesap (diğer yöntemlerde null). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hesap_id")
    private Hesap hesap;

    @Column(name = "odeme_tarihi", nullable = false)
    private LocalDateTime odemeTarihi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kullanici_id")
    private Kullanici kullanici;
}
