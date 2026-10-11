package com.msc.halisaha.repository;

import com.msc.halisaha.entity.Yoklama;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface YoklamaRepository extends JpaRepository<Yoklama, Long> {

    List<Yoklama> findByIsletmeIdAndTarihAndOgrenciIdIn(Long isletmeId, LocalDate tarih, Collection<Long> ogrenciIds);
}
