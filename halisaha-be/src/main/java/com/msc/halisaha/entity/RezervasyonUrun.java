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

@Entity
@Table(name = "rezervasyon_urun")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RezervasyonUrun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rezervasyon_id", nullable = false)
    private Rezervasyon rezervasyon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "urun_id", nullable = false)
    private Urun urun;

    @Column(name = "miktar", nullable = false)
    private Integer miktar;
}
