package com.msc.halisaha.repository;

import com.msc.halisaha.entity.EnvanterTur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnvanterTurRepository extends JpaRepository<EnvanterTur, Long> {

    List<EnvanterTur> findByIsletmeId(Long isletmeId);

    Optional<EnvanterTur> findByIdAndIsletmeId(Long id, Long isletmeId);

    boolean existsByIsletmeIdAndAdIgnoreCase(Long isletmeId, String ad);

    boolean existsByIsletmeIdAndAdIgnoreCaseAndIdNot(Long isletmeId, String ad, Long id);

    @Query("SELECT DISTINCT e.tur.id FROM Envanter e WHERE e.isletme.id = :isletmeId")
    List<Long> kullanimdakiIdler(@Param("isletmeId") Long isletmeId);
}
