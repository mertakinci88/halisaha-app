package com.msc.halisaha.repository;

import com.msc.halisaha.entity.Ogrenci;
import com.msc.halisaha.entity.OgrenciDurum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OgrenciRepository extends JpaRepository<Ogrenci, Long> {

    Optional<Ogrenci> findByIdAndIsletmeId(Long id, Long isletmeId);

    List<Ogrenci> findByIsletmeIdAndGrupIdAndDurum(Long isletmeId, Long grupId, OgrenciDurum durum);

    @Query("""
            SELECT o FROM Ogrenci o LEFT JOIN o.grup g
            WHERE o.isletme.id = :isletmeId
              AND (:grupId IS NULL OR g.id = :grupId)
              AND (:odemeBekleyen = FALSE OR NOT EXISTS (
                     SELECT 1 FROM OgrenciOdeme p WHERE p.ogrenci = o AND p.yil = :yil AND p.ay = :ay AND p.odenenTutar > 0))
              AND (:q IS NULL OR :q = ''
                   OR LOWER(o.adSoyad) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(o.veliAdSoyad) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(g.ad) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Ogrenci> ara(@Param("isletmeId") Long isletmeId, @Param("grupId") Long grupId, @Param("q") String q,
                      @Param("odemeBekleyen") boolean odemeBekleyen, @Param("yil") int yil, @Param("ay") int ay,
                      Pageable pageable);
}
