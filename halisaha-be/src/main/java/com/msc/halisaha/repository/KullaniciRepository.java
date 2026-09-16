package com.msc.halisaha.repository;

import com.msc.halisaha.entity.Kullanici;
import com.msc.halisaha.entity.KullaniciDurum;
import com.msc.halisaha.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KullaniciRepository extends JpaRepository<Kullanici, Long> {

    Optional<Kullanici> findByKullaniciAdi(String kullaniciAdi);

    long countByRolAndDurum(Rol rol, KullaniciDurum durum);
}
