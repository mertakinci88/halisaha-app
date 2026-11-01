package com.msc.halisaha.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * İşletmenin sattığı envanter türü (ör. Yazlık Forma). Bedenli türlerin geçerli bedenleri {@link #bedenler} listesindedir
 * (liste sırası ekrandaki seçim sırasıdır); bedensiz türlerde envanter bedeni STANDART'tır.
 */
@Entity
@Table(name = "envanter_tur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnvanterTur {

    public static final String STANDART = "STANDART";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "isletme_id", nullable = false)
    private Isletme isletme;

    @Column(name = "ad", nullable = false, length = 60)
    private String ad;

    /** Bedenli türlerde beden seçilir; diğerlerinde beden STANDART tutulur. */
    @Column(name = "bedenli", nullable = false)
    private boolean bedenli;

    /** Stok birimi: "adet" ya da "çift" (ör. tozluk). */
    @Builder.Default
    @Column(name = "birim", nullable = false, length = 10)
    private String birim = "adet";

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "envanter_tur_beden", joinColumns = @JoinColumn(name = "tur_id"))
    @OrderColumn(name = "sira")
    @Column(name = "beden", nullable = false, length = 10)
    private List<String> bedenler = new ArrayList<>();

    public boolean bedenGecerli(String beden) {
        return beden != null && bedenler.contains(beden);
    }

    /** Sıralama anahtarı: STANDART başa, sonra türün tanımlı beden sırası. */
    public int bedenSira(String beden) {
        return STANDART.equals(beden) ? -1 : bedenler.indexOf(beden);
    }
}
