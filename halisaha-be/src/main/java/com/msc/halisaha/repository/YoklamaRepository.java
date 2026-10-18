package com.msc.halisaha.repository;

import com.msc.halisaha.entity.Yoklama;
import com.msc.halisaha.entity.YoklamaDurum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface YoklamaRepository extends JpaRepository<Yoklama, Long> {

    List<Yoklama> findByIsletmeIdAndTarihAndOgrenciIdIn(Long isletmeId, LocalDate tarih, Collection<Long> ogrenciIds);

    Page<Yoklama> findByIsletmeIdAndOgrenciId(Long isletmeId, Long ogrenciId, Pageable pageable);

    long countByIsletmeIdAndOgrenciIdAndDurum(Long isletmeId, Long ogrenciId, YoklamaDurum durum);
}
