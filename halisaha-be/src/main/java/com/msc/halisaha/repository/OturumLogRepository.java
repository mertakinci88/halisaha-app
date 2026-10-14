package com.msc.halisaha.repository;

import com.msc.halisaha.entity.OturumLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface OturumLogRepository extends JpaRepository<OturumLog, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM OturumLog l WHERE l.zaman < :sinir")
    int eskileriSil(@Param("sinir") LocalDateTime sinir);
}
