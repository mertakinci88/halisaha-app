package com.msc.halisaha.repository;

import com.msc.halisaha.entity.OgrenciTransfer;
import com.msc.halisaha.entity.TransferDurum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OgrenciTransferRepository extends JpaRepository<OgrenciTransfer, Long> {

    Optional<OgrenciTransfer> findByIdAndIsletmeId(Long id, Long isletmeId);

    boolean existsByOgrenciIdAndDurum(Long ogrenciId, TransferDurum durum);

    /** Öğrencinin transfer geçmişi (en yeni talep önce). */
    List<OgrenciTransfer> findByOgrenciIdAndIsletmeIdOrderByTalepTarihiDescIdDesc(Long ogrenciId, Long isletmeId);

    /** durum boşsa hepsi; en yeni talep önce. */
    @Query("""
            SELECT t FROM OgrenciTransfer t
            WHERE t.isletme.id = :isletmeId AND (:durum IS NULL OR t.durum = :durum)
            ORDER BY t.talepTarihi DESC, t.id DESC
            """)
    List<OgrenciTransfer> listele(@Param("isletmeId") Long isletmeId, @Param("durum") TransferDurum durum);

    long countByIsletmeIdAndDurum(Long isletmeId, TransferDurum durum);
}
