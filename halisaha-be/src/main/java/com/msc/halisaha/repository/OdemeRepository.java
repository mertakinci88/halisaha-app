package com.msc.halisaha.repository;

import com.msc.halisaha.entity.Odeme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OdemeRepository extends JpaRepository<Odeme, Long> {

    @Query("SELECT o FROM Odeme o LEFT JOIN FETCH o.kullanici WHERE o.rezervasyon.id = :rezervasyonId ORDER BY o.odemeTarihi, o.id")
    List<Odeme> findByRezervasyonId(@Param("rezervasyonId") Long rezervasyonId);

    @Query("SELECT o FROM Odeme o LEFT JOIN FETCH o.kullanici WHERE o.rezervasyon.id IN :ids ORDER BY o.odemeTarihi, o.id")
    List<Odeme> findByRezervasyonIdIn(@Param("ids") Collection<Long> ids);

    boolean existsByRezervasyonId(Long rezervasyonId);
}
