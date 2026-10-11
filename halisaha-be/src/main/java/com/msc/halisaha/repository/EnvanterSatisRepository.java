package com.msc.halisaha.repository;

import com.msc.halisaha.entity.EnvanterSatis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnvanterSatisRepository extends JpaRepository<EnvanterSatis, Long> {

    @Query("""
            SELECT DISTINCT s FROM EnvanterSatis s
            JOIN FETCH s.kalemler k JOIN FETCH k.envanter
            WHERE s.isletme.id = :isletmeId AND s.ogrenci.id = :ogrenciId
            ORDER BY s.id
            """)
    List<EnvanterSatis> listele(@Param("isletmeId") Long isletmeId, @Param("ogrenciId") Long ogrenciId);

    @Query("""
            SELECT s FROM EnvanterSatis s
            JOIN FETCH s.kalemler k JOIN FETCH k.envanter
            WHERE s.id = :id AND s.isletme.id = :isletmeId AND s.ogrenci.id = :ogrenciId
            """)
    Optional<EnvanterSatis> bul(@Param("id") Long id, @Param("isletmeId") Long isletmeId, @Param("ogrenciId") Long ogrenciId);

    /** Set silinirken satışların set bağlantısını kaldırır (set_ad kopyası kalır). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE EnvanterSatis s SET s.set = NULL WHERE s.set.id = :setId")
    int setBaglantisiniKaldir(@Param("setId") Long setId);

    boolean existsByOgrenciId(Long ogrenciId);
}
