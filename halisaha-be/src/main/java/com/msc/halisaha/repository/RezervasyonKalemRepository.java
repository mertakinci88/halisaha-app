package com.msc.halisaha.repository;

import com.msc.halisaha.entity.RezervasyonKalem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RezervasyonKalemRepository extends JpaRepository<RezervasyonKalem, Long> {

    List<RezervasyonKalem> findByRezervasyonIdOrderByIdAsc(Long rezervasyonId);

    List<RezervasyonKalem> findByRezervasyonIdInOrderByIdAsc(Collection<Long> rezervasyonIds);
}
