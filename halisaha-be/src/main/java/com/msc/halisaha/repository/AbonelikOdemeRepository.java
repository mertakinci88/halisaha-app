package com.msc.halisaha.repository;

import com.msc.halisaha.entity.AbonelikOdeme;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AbonelikOdemeRepository extends JpaRepository<AbonelikOdeme, Long> {

    boolean existsByHesapId(Long hesapId);
}
