package com.msc.halisaha.repository;

import com.msc.halisaha.entity.OgrenciOdeme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OgrenciOdemeRepository extends JpaRepository<OgrenciOdeme, Long> {

    @Query("SELECT o FROM OgrenciOdeme o LEFT JOIN FETCH o.hesap WHERE o.ogrenci.id = :ogrenciId AND o.yil = :yil ORDER BY o.ay, o.odemeTarihi, o.id")
    List<OgrenciOdeme> findByOgrenciIdAndYil(@Param("ogrenciId") Long ogrenciId, @Param("yil") int yil);

    @Query("SELECT o FROM OgrenciOdeme o LEFT JOIN FETCH o.hesap WHERE o.id = :id AND o.ogrenci.id = :ogrenciId")
    Optional<OgrenciOdeme> findByIdAndOgrenciId(@Param("id") Long id, @Param("ogrenciId") Long ogrenciId);

    /** Öğrencilerin verilen aydaki ödemeleri (durum hesabı için; ödemesiz not kayıtları dahil). */
    @Query("SELECT o FROM OgrenciOdeme o WHERE o.ogrenci.id IN :ogrenciIdleri AND o.yil = :yil AND o.ay = :ay")
    List<OgrenciOdeme> findDonem(@Param("ogrenciIdleri") Collection<Long> ogrenciIdleri, @Param("yil") int yil, @Param("ay") int ay);

    /** Kayıtlı ödemelerin yılları (yıl seçicisi için). */
    @Query("SELECT DISTINCT o.yil FROM OgrenciOdeme o WHERE o.ogrenci.id = :ogrenciId ORDER BY o.yil")
    List<Integer> yillar(@Param("ogrenciId") Long ogrenciId);

    boolean existsByHesapId(Long hesapId);
}
