package com.msc.halisaha.repository;

import com.msc.halisaha.entity.Iade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface IadeRepository extends JpaRepository<Iade, Long> {

    @Query("SELECT i FROM Iade i LEFT JOIN FETCH i.hesap LEFT JOIN FETCH i.kullanici WHERE i.rezervasyon.id = :id ORDER BY i.iadeTarihi, i.id")
    List<Iade> rezervasyonIadeleri(@Param("id") Long id);

    @Query("SELECT i FROM Iade i LEFT JOIN FETCH i.hesap LEFT JOIN FETCH i.kullanici WHERE i.ogrenciOdeme.id = :id ORDER BY i.iadeTarihi, i.id")
    List<Iade> ogrenciOdemeIadeleri(@Param("id") Long id);

    @Query("SELECT i FROM Iade i LEFT JOIN FETCH i.hesap LEFT JOIN FETCH i.kullanici WHERE i.envanterSatis.id = :id ORDER BY i.iadeTarihi, i.id")
    List<Iade> envanterSatisIadeleri(@Param("id") Long id);

    @Query("SELECT i FROM Iade i LEFT JOIN FETCH i.hesap LEFT JOIN FETCH i.kullanici WHERE i.abonelik.id = :id ORDER BY i.iadeTarihi, i.id")
    List<Iade> abonelikIadeleri(@Param("id") Long id);

    /** Bir öğrencinin seçili yıldaki aylık ödemelerine yapılmış iadeler. */
    @Query("""
            SELECT i FROM Iade i LEFT JOIN FETCH i.hesap LEFT JOIN FETCH i.kullanici
            WHERE i.ogrenciOdeme.ogrenci.id = :ogrenciId AND i.ogrenciOdeme.yil = :yil AND i.isletme.id = :isletmeId
            ORDER BY i.iadeTarihi, i.id
            """)
    List<Iade> ogrenciYilIadeleri(@Param("ogrenciId") Long ogrenciId, @Param("yil") int yil, @Param("isletmeId") Long isletmeId);

    @Query("SELECT COALESCE(SUM(i.tutar), 0) FROM Iade i WHERE i.ogrenciOdeme.id = :id")
    BigDecimal ogrenciOdemeToplami(@Param("id") Long id);

    @Query("SELECT COALESCE(SUM(i.tutar), 0) FROM Iade i WHERE i.envanterSatis.id = :id")
    BigDecimal envanterSatisToplami(@Param("id") Long id);

    @Query("SELECT COALESCE(SUM(i.tutar), 0) FROM Iade i WHERE i.abonelik.id = :id")
    BigDecimal abonelikToplami(@Param("id") Long id);

    /** İşletmedeki aboneliklerin iade toplamları: [abonelikId, toplam]; iadesi olmayan abonelik satırda yoktur. */
    @Query("SELECT i.abonelik.id, SUM(i.tutar) FROM Iade i WHERE i.isletme.id = :isletmeId AND i.abonelik IS NOT NULL GROUP BY i.abonelik.id")
    List<Object[]> abonelikToplamlari(@Param("isletmeId") Long isletmeId);

    @Query("SELECT COALESCE(SUM(i.tutar), 0) FROM Iade i WHERE i.rezervasyon.id = :id")
    BigDecimal rezervasyonToplami(@Param("id") Long id);

    @Query("SELECT i FROM Iade i LEFT JOIN FETCH i.hesap LEFT JOIN FETCH i.kullanici WHERE i.id = :id AND i.isletme.id = :isletmeId")
    Optional<Iade> bul(@Param("id") Long id, @Param("isletmeId") Long isletmeId);

    boolean existsByHesapId(Long hesapId);
}
