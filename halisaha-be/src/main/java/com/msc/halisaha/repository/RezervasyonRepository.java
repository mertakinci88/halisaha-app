package com.msc.halisaha.repository;

import com.msc.halisaha.entity.Rezervasyon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RezervasyonRepository extends JpaRepository<Rezervasyon, Long> {

    List<Rezervasyon> findBySahaId(Long sahaId);

    boolean existsBySahaId(Long sahaId);

    List<Rezervasyon> findBySahaIdAndBaslangicTarihBetween(Long sahaId, LocalDateTime start, LocalDateTime end);

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
