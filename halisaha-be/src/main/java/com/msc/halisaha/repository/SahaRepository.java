package com.msc.halisaha.repository;

import com.msc.halisaha.entity.Saha;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SahaRepository extends JpaRepository<Saha, Long> {

    List<Saha> findByIsletmeId(Long isletmeId);

    Optional<Saha> findByIdAndIsletmeId(Long id, Long isletmeId);
}
