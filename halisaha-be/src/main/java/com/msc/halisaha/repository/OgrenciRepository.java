package com.msc.halisaha.repository;

import com.msc.halisaha.entity.Ogrenci;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OgrenciRepository extends JpaRepository<Ogrenci, Long> {

    List<Ogrenci> findByGrupId(Long grupId);
}
