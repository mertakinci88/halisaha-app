package com.msc.halisaha.repository;

import com.msc.halisaha.entity.Abonelik;
import com.msc.halisaha.entity.AbonelikDurum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AbonelikRepository extends JpaRepository<Abonelik, Long> {

    @Query("""
            SELECT DISTINCT a FROM Abonelik a
            JOIN FETCH a.saha LEFT JOIN FETCH a.kullanici
            WHERE a.isletme.id = :isletmeId AND (:durum IS NULL OR a.durum = :durum)
            """)
    List<Abonelik> listele(@Param("isletmeId") Long isletmeId, @Param("durum") AbonelikDurum durum);

    @Query("""
            SELECT a FROM Abonelik a JOIN FETCH a.saha LEFT JOIN FETCH a.kullanici
            WHERE a.id = :id AND a.isletme.id = :isletmeId
            """)
    Optional<Abonelik> bul(@Param("id") Long id, @Param("isletmeId") Long isletmeId);

    boolean existsBySahaId(Long sahaId);
}
