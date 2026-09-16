package com.msc.halisaha.repository;

import com.msc.halisaha.entity.Ogrenci;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OgrenciRepository extends JpaRepository<Ogrenci, Long> {

    List<Ogrenci> findByGrupId(Long grupId);

    @Query("""
            SELECT o FROM Ogrenci o LEFT JOIN o.grup g
            WHERE (:grupId IS NULL OR g.id = :grupId)
              AND (:q IS NULL OR :q = ''
                   OR LOWER(o.adSoyad) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(o.veliAdSoyad) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(g.ad) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Ogrenci> ara(@Param("grupId") Long grupId, @Param("q") String q, Pageable pageable);
}
