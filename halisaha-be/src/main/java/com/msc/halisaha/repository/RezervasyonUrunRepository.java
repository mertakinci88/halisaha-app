package com.msc.halisaha.repository;

import com.msc.halisaha.entity.RezervasyonUrun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RezervasyonUrunRepository extends JpaRepository<RezervasyonUrun, Long> {

    List<RezervasyonUrun> findByRezervasyonId(Long rezervasyonId);
}
