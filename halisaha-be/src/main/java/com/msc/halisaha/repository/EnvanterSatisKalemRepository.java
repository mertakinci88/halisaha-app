package com.msc.halisaha.repository;

import com.msc.halisaha.entity.EnvanterSatisKalem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EnvanterSatisKalemRepository extends JpaRepository<EnvanterSatisKalem, Long> {

    boolean existsByEnvanterId(Long envanterId);

    /** Ödemesi alınmış ama henüz teslim edilmemiş kalemler, en eski satıştan başlayarak. */
    @Query("""
            SELECT k FROM EnvanterSatisKalem k
            JOIN FETCH k.envanter JOIN FETCH k.satis s JOIN FETCH s.ogrenci
            WHERE s.isletme.id = :isletmeId AND k.odendi = TRUE AND k.teslimEdildi = FALSE
            ORDER BY s.kayitTarihi, s.id, k.id
            """)
    List<EnvanterSatisKalem> teslimBekleyenler(@Param("isletmeId") Long isletmeId);
}
