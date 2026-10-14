package com.msc.halisaha.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** İşletmenin standart abonelik ücreti; kayıt formunda ücret alanını doldurur. */
@Entity
@Table(name = "abonelik_ayar")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AbonelikAyar {

    @Id
    @Column(name = "isletme_id")
    private Long isletmeId;

    @Column(name = "standart_ucret", nullable = false, precision = 10, scale = 2)
    private BigDecimal standartUcret;
}
