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

/** Öğrencinin bir aya ait ödeme kaydı. Yalnızca ödeme alınmış ya da not girilmiş aylar için kayıt vardır. */
@Entity
@Table(name = "ogrenci_odeme")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OgrenciOdeme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Bağlı olduğu kaydın işletmesi (raporların işletmeye göre hızlı okunması için); kayıt eklenirken otomatik doldurulur. */
    @Column(name = "isletme_id", nullable = false, updatable = false)
    private Long isletmeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ogrenci_id", nullable = false)
    private Ogrenci ogrenci;

    @Column(name = "yil", nullable = false)
    private int yil;

    @Column(name = "ay", nullable = false)
    private int ay;

    /** Ödenen tutar; 0 ise yalnızca not vardır (ay ödenmiş sayılmaz). */
    @Column(name = "odenen_tutar", nullable = false, precision = 10, scale = 2)
    private BigDecimal odenenTutar;

    /** Tutar girilmişse zorunlu. */
    @Enumerated(EnumType.STRING)
    @Column(name = "odeme_sekli", length = 20)
    private OdemeYontemi odemeSekli;

    /** Havale / EFT ödemede paranın yattığı hesap (diğer yöntemlerde null). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hesap_id")
    private Hesap hesap;

    @Column(name = "odeme_tarihi")
    private LocalDate odemeTarihi;

    @Column(name = "\"not\"", length = 255)
    private String not;

    /** Ödemeyi kaydeden kullanıcı. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kullanici_id")
    private Kullanici kullanici;

    @Column(name = "kayit_tarih", nullable = false, updatable = false)
    private LocalDateTime kayitTarih;

    @PrePersist
    void prePersist() {
        if (kayitTarih == null) {
            kayitTarih = LocalDateTime.now();
        }
        if (isletmeId == null) {
            isletmeId = getOgrenci().getIsletme().getId();
        }
    }
}
