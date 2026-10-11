package com.msc.halisaha.repository;

import com.msc.halisaha.entity.Rezervasyon;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RezervasyonRepository extends JpaRepository<Rezervasyon, Long> {

    @EntityGraph(attributePaths = {"saha", "kullanici"})
    List<Rezervasyon> findByIsletmeId(Long isletmeId);

    Optional<Rezervasyon> findByIdAndIsletmeId(Long id, Long isletmeId);

    @EntityGraph(attributePaths = {"saha", "kullanici"})
    List<Rezervasyon> findByIsletmeIdAndSahaId(Long isletmeId, Long sahaId);

    boolean existsBySahaId(Long sahaId);

    @EntityGraph(attributePaths = {"saha", "kullanici"})
    List<Rezervasyon> findByIsletmeIdAndSahaIdAndBaslangicTarihGreaterThanEqualAndBaslangicTarihLessThan(
            Long isletmeId, Long sahaId, LocalDateTime start, LocalDateTime end);

    @EntityGraph(attributePaths = {"saha", "kullanici"})
    List<Rezervasyon> findByIsletmeIdAndBaslangicTarihGreaterThanEqualAndBaslangicTarihLessThan(
            Long isletmeId, LocalDateTime start, LocalDateTime end);

    @Query("""
            SELECT r FROM Rezervasyon r
            WHERE r.saha.id = :sahaId
              AND r.durum = com.msc.halisaha.entity.RezervasyonDurum.AKTIF
              AND r.baslangicTarih < :bitis
              AND r.bitisTarih > :baslangic
              AND (:excludeId IS NULL OR r.id <> :excludeId)
            """)
    List<Rezervasyon> findOverlapping(
            @Param("sahaId") Long sahaId,
            @Param("baslangic") LocalDateTime baslangic,
            @Param("bitis") LocalDateTime bitis,
            @Param("excludeId") Long excludeId
    );
}
