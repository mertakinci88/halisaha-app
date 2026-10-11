package com.msc.halisaha.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** Satış setindeki bir envanter. Stok yalnızca teslimEdildi = true iken düşmüş sayılır. */
@Entity
@Table(name = "envanter_satis_kalem")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnvanterSatisKalem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "satis_id", nullable = false)
    private EnvanterSatis satis;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "envanter_id", nullable = false)
    private Envanter envanter;

    /** Satış anındaki fiyatın kopyası; envanter fiyatı sonradan değişse de geçmiş satış değişmez. */
    @Column(name = "birim_fiyat", nullable = false, precision = 10, scale = 2)
    private BigDecimal birimFiyat;

    @Column(name = "odendi", nullable = false)
    private boolean odendi;

    @Column(name = "teslim_edildi", nullable = false)
    private boolean teslimEdildi;
}
