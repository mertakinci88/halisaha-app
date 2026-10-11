package com.msc.halisaha.repository;

import com.msc.halisaha.entity.Grup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GrupRepository extends JpaRepository<Grup, Long> {

    List<Grup> findByIsletmeId(Long isletmeId);

    Optional<Grup> findByIdAndIsletmeId(Long id, Long isletmeId);
}
