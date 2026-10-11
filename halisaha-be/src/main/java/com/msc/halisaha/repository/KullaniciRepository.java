package com.msc.halisaha.repository;

import com.msc.halisaha.entity.Kullanici;
import com.msc.halisaha.entity.KullaniciDurum;
import com.msc.halisaha.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KullaniciRepository extends JpaRepository<Kullanici, Long> {

    Optional<Kullanici> findByKullaniciAdi(String kullaniciAdi);

    List<Kullanici> findByIsletmeId(Long isletmeId);

    Optional<Kullanici> findByIdAndIsletmeId(Long id, Long isletmeId);

    long countByIsletmeIdAndRolAndDurum(Long isletmeId, Rol rol, KullaniciDurum durum);

    boolean existsByRol(Rol rol);
}
