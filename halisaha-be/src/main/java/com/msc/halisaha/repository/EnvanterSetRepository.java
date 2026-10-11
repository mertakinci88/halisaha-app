package com.msc.halisaha.repository;

import com.msc.halisaha.entity.EnvanterSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnvanterSetRepository extends JpaRepository<EnvanterSet, Long> {

    @Query("""
            SELECT DISTINCT s FROM EnvanterSet s LEFT JOIN FETCH s.envanterler
            WHERE s.isletme.id = :isletmeId
            """)
    List<EnvanterSet> listele(@Param("isletmeId") Long isletmeId);

    @Query("""
            SELECT s FROM EnvanterSet s LEFT JOIN FETCH s.envanterler
            WHERE s.id = :id AND s.isletme.id = :isletmeId
            """)
    Optional<EnvanterSet> bul(@Param("id") Long id, @Param("isletmeId") Long isletmeId);

    boolean existsByIsletmeIdAndAdIgnoreCase(Long isletmeId, String ad);

    boolean existsByIsletmeIdAndAdIgnoreCaseAndIdNot(Long isletmeId, String ad, Long id);

    /** Envanter kaydı herhangi bir sette kullanılıyor mu (silme kontrolü). */
    @Query("SELECT COUNT(s) > 0 FROM EnvanterSet s JOIN s.envanterler e WHERE e.id = :envanterId")
    boolean envanterKullaniliyor(@Param("envanterId") Long envanterId);
}
