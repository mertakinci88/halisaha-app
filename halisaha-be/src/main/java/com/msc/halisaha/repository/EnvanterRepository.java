package com.msc.halisaha.repository;

import com.msc.halisaha.entity.Envanter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnvanterRepository extends JpaRepository<Envanter, Long> {

    List<Envanter> findByIsletmeId(Long isletmeId);

    Optional<Envanter> findByIdAndIsletmeId(Long id, Long isletmeId);

    Optional<Envanter> findByIsletmeIdAndTurIdAndBeden(Long isletmeId, Long turId, String beden);

    boolean existsByIsletmeIdAndTurIdAndBedenAndIdNot(Long isletmeId, Long turId, String beden, Long id);

    boolean existsByIsletmeIdAndTurIdAndBeden(Long isletmeId, Long turId, String beden);

    boolean existsByTurId(Long turId);

    boolean existsByTurIdAndBeden(Long turId, String beden);

    /** Stoğu tek SQL ile atomik düşer; stok yetersizse 0 satır etkilenir (stok eksiye düşmez). */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Envanter e SET e.adet = e.adet - 1 WHERE e.id = :id AND e.adet >= 1")
    int stokDus(@Param("id") Long id);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE Envanter e SET e.adet = e.adet + 1 WHERE e.id = :id")
    int stokEkle(@Param("id") Long id);

    /** Veritabanındaki güncel adet (persistence context'teki eski değeri değil). */
    @Query("SELECT e.adet FROM Envanter e WHERE e.id = :id")
    Integer mevcutAdet(@Param("id") Long id);
}
