package com.msc.halisaha.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Giriş / çıkış / başarısız giriş kaydı. Başarısız denemede kullanıcı adı yanlışsa kullanıcı ve işletme boştur. */
@Entity
@Table(name = "oturum_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OturumLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "isletme_id")
    private Long isletmeId;

    @Column(name = "kullanici_id")
    private Long kullaniciId;

    @Column(name = "kullanici_adi", nullable = false, length = 100)
    private String kullaniciAdi;

    @Enumerated(EnumType.STRING)
    @Column(name = "olay", nullable = false, length = 20)
    private OturumOlay olay;

    @Column(name = "zaman", nullable = false)
    private LocalDateTime zaman;

    @Column(name = "ip", length = 64)
    private String ip;

    @Column(name = "tarayici", length = 255)
    private String tarayici;
}
