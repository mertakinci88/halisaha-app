package com.msc.halisaha.repository;

import com.msc.halisaha.entity.Grup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GrupRepository extends JpaRepository<Grup, Long> {

    List<Grup> findByIsletmeId(Long isletmeId);

    Optional<Grup> findByIdAndIsletmeId(Long id, Long isletmeId);

    /** Adında aranan metin geçen grupların id'leri (öğrenci aramasında grup adıyla eşleşme için). */
    @Query("SELECT g.id FROM Grup g WHERE g.isletme.id = :isletmeId AND LOWER(g.ad) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Long> adaGoreIdler(@Param("isletmeId") Long isletmeId, @Param("q") String q);
}
