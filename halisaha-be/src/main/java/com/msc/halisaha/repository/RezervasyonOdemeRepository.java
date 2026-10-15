package com.msc.halisaha.repository;

import com.msc.halisaha.entity.RezervasyonOdeme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface RezervasyonOdemeRepository extends JpaRepository<RezervasyonOdeme, Long> {

    @Query("SELECT o FROM RezervasyonOdeme o LEFT JOIN FETCH o.kullanici LEFT JOIN FETCH o.hesap WHERE o.rezervasyon.id = :rezervasyonId ORDER BY o.odemeTarihi, o.id")
    List<RezervasyonOdeme> findByRezervasyonId(@Param("rezervasyonId") Long rezervasyonId);

    @Query("SELECT o FROM RezervasyonOdeme o LEFT JOIN FETCH o.kullanici LEFT JOIN FETCH o.hesap WHERE o.rezervasyon.id IN :ids ORDER BY o.odemeTarihi, o.id")
    List<RezervasyonOdeme> findByRezervasyonIdIn(@Param("ids") Collection<Long> ids);

    /** Verilen rezervasyonların alınan ödeme toplamları: [rezervasyonId, toplam]; ödemesi olmayan satırda yoktur. */
    @Query("SELECT o.rezervasyon.id, SUM(o.tutar) FROM RezervasyonOdeme o WHERE o.rezervasyon.id IN :ids GROUP BY o.rezervasyon.id")
    List<Object[]> toplamlar(@Param("ids") Collection<Long> ids);

    boolean existsByRezervasyonId(Long rezervasyonId);

    boolean existsByHesapId(Long hesapId);
}
