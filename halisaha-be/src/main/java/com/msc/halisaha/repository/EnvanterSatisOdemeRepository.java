package com.msc.halisaha.repository;

import com.msc.halisaha.entity.EnvanterSatisOdeme;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnvanterSatisOdemeRepository extends JpaRepository<EnvanterSatisOdeme, Long> {

    boolean existsByHesapId(Long hesapId);
}
