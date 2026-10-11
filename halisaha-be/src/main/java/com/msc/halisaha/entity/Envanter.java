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

/** Bir envanter türünün bir bedendeki stok kaydı (ör. Yazlık Forma / Medium). Bedensiz türlerde beden STANDART'tır. */
@Entity
@Table(name = "envanter")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Envanter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "isletme_id", nullable = false)
    private Isletme isletme;

    @Enumerated(EnumType.STRING)
    @Column(name = "tur", nullable = false, length = 30)
    private EnvanterTur tur;

    @Column(name = "beden", nullable = false, length = 10)
    private String beden;

    @Column(name = "adet", nullable = false)
    private Integer adet;

    /** Stok bu adetten azsa "Stok az" uyarısı verilir. */
    @Builder.Default
    @Column(name = "stok_uyari_adedi", nullable = false)
    private Integer stokUyariAdedi = 10;

    @Column(name = "satis_fiyati", nullable = false, precision = 10, scale = 2)
    private BigDecimal satisFiyati;
}
