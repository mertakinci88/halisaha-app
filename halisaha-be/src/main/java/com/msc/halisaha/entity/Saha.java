package com.msc.halisaha.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "saha")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Saha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ad", nullable = false)
    private String ad;

    @Enumerated(EnumType.STRING)
    @Column(name = "durum", nullable = false, length = 20)
    private SahaDurum durum;

    @Column(name = "saatlik_ucret", nullable = false, precision = 10, scale = 2)
    private BigDecimal saatlikUcret;

    @PrePersist
    void prePersist() {
        if (durum == null) {
            durum = SahaDurum.AKTIF;
        }
    }
}
