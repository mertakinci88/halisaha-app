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
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Aboneliğe alınan tek bir ödeme. */
@Entity
@Table(name = "abonelik_odeme")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbonelikOdeme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Bağlı olduğu kaydın işletmesi (raporların işletmeye göre hızlı okunması için); kayıt eklenirken otomatik doldurulur. */
    @Column(name = "isletme_id", nullable = false, updatable = false)
    private Long isletmeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "abonelik_id", nullable = false)
    private Abonelik abonelik;

    @Column(name = "tutar", nullable = false, precision = 10, scale = 2)
    private BigDecimal tutar;

    @Enumerated(EnumType.STRING)
    @Column(name = "odeme_sekli", nullable = false, length = 20)
    private OdemeYontemi odemeSekli;

    /** Havale / EFT ödemede paranın yattığı hesap (diğer yöntemlerde null). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hesap_id")
    private Hesap hesap;

    @Column(name = "odeme_tarihi", nullable = false)
    private LocalDate odemeTarihi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kullanici_id")
    private Kullanici kullanici;

    @Column(name = "kayit_tarih", nullable = false, updatable = false)
    private LocalDateTime kayitTarih;

    @PrePersist
    void isletmeyiBagla() {
        if (isletmeId == null) {
            isletmeId = getAbonelik().getIsletme().getId();
        }
    }
}
