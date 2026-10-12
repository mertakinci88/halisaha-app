package com.msc.halisaha.repository;

import com.msc.halisaha.entity.Hesap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HesapRepository extends JpaRepository<Hesap, Long> {

    List<Hesap> findByIsletmeId(Long isletmeId);

    Optional<Hesap> findByIdAndIsletmeId(Long id, Long isletmeId);
}
