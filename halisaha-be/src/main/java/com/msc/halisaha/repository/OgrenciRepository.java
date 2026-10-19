package com.msc.halisaha.repository;

import com.msc.halisaha.entity.EnvanterTur;
import com.msc.halisaha.entity.Ogrenci;
import com.msc.halisaha.entity.OgrenciDurum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OgrenciRepository extends JpaRepository<Ogrenci, Long> {

    Optional<Ogrenci> findByIdAndIsletmeId(Long id, Long isletmeId);

    List<Ogrenci> findByIsletmeIdAndGrupIdAndDurum(Long isletmeId, Long grupId, OgrenciDurum durum);

    @Query("""
            SELECT o FROM Ogrenci o LEFT JOIN o.grup g
            WHERE o.isletme.id = :isletmeId
              AND (:grupId IS NULL OR g.id = :grupId)
              AND (:kisitli = FALSE OR g.id IN :izinliGruplar)
              AND (:durum IS NULL OR o.durum = :durum)
              AND (:tamamlanmis = FALSE OR EXISTS (
                     SELECT 1 FROM OgrenciOdeme p WHERE p.ogrenci = o AND p.yil = :yil AND p.ay = :ay AND p.tamam = TRUE))
              AND (:tamamlanmamis = FALSE OR NOT EXISTS (
                     SELECT 1 FROM OgrenciOdeme p WHERE p.ogrenci = o AND p.yil = :yil AND p.ay = :ay AND p.tamam = TRUE))
              AND (:tutarDurumu = 0
                   OR (:tutarDurumu = 1 AND NOT EXISTS (
                         SELECT 1 FROM OgrenciOdeme p WHERE p.ogrenci = o AND p.yil = :yil AND p.ay = :ay AND p.odenenTutar > 0))
                   OR (:tutarDurumu = 2 AND EXISTS (
                         SELECT 1 FROM OgrenciOdeme p WHERE p.ogrenci = o AND p.yil = :yil AND p.ay = :ay AND p.odenenTutar > 0)))
              AND o.kayitTarih >= :kayitBas
              AND (:antrenmanGun IS NULL OR o.antrenmanGunSayisi = :antrenmanGun)
              AND (:transferBekleyen = FALSE OR EXISTS (
                     SELECT 1 FROM OgrenciTransfer t WHERE t.ogrenci = o AND t.durum = com.msc.halisaha.entity.TransferDurum.BEKLIYOR))
              AND (:devamsizlik = 0 OR (
                     SELECT COUNT(y) FROM Yoklama y WHERE y.ogrenci = o AND y.durum = com.msc.halisaha.entity.YoklamaDurum.GELMEDI
                       AND y.tarih >= :devamsizlikBas) >= :devamsizlik)
              AND (:envanterFiltre = ''
                   OR (:envanterFiltre = 'SAHIP' AND EXISTS (
                         SELECT 1 FROM EnvanterSatisKalem k WHERE k.satis.ogrenci = o AND (:envanterTur IS NULL OR k.envanter.tur = :envanterTur)))
                   OR (:envanterFiltre = 'TESLIM_BEKLEYEN' AND EXISTS (
                         SELECT 1 FROM EnvanterSatisKalem k WHERE k.satis.ogrenci = o AND k.odendi = TRUE AND k.teslimEdildi = FALSE
                           AND (:envanterTur IS NULL OR k.envanter.tur = :envanterTur)))
                   OR (:envanterFiltre = 'BORCLU' AND EXISTS (
                         SELECT 1 FROM EnvanterSatisKalem k WHERE k.satis.ogrenci = o AND k.satis.odenenTutar < k.satis.setTutari
                           AND (:envanterTur IS NULL OR k.envanter.tur = :envanterTur)))
                   OR (:envanterFiltre = 'ALMAMIS' AND NOT EXISTS (
                         SELECT 1 FROM EnvanterSatisKalem k WHERE k.satis.ogrenci = o AND (:envanterTur IS NULL OR k.envanter.tur = :envanterTur)))
                   OR (:envanterFiltre = 'TAMAM'
                         AND EXISTS (SELECT 1 FROM EnvanterSatisKalem k WHERE k.satis.ogrenci = o AND (:envanterTur IS NULL OR k.envanter.tur = :envanterTur))
                         AND NOT EXISTS (SELECT 1 FROM EnvanterSatis s WHERE s.ogrenci = o AND s.odenenTutar < s.setTutari)
                         AND NOT EXISTS (SELECT 1 FROM EnvanterSatisKalem k WHERE k.satis.ogrenci = o AND k.odendi = TRUE AND k.teslimEdildi = FALSE)))
              AND (:q IS NULL OR :q = ''
                   OR LOWER(o.adSoyad) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(o.veliAdSoyad) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR o.grup.id IN :arananGruplar)
            """)
    Page<Ogrenci> ara(@Param("isletmeId") Long isletmeId, @Param("grupId") Long grupId, @Param("q") String q,
                      @Param("durum") OgrenciDurum durum,
                      @Param("tamamlanmis") boolean tamamlanmis, @Param("tamamlanmamis") boolean tamamlanmamis, @Param("tutarDurumu") int tutarDurumu,
                      @Param("envanterFiltre") String envanterFiltre, @Param("envanterTur") EnvanterTur envanterTur,
                      @Param("kayitBas") LocalDateTime kayitBas, @Param("antrenmanGun") Integer antrenmanGun,
                      @Param("transferBekleyen") boolean transferBekleyen,
                      @Param("devamsizlik") int devamsizlik, @Param("devamsizlikBas") LocalDate devamsizlikBas,
                      @Param("yil") int yil, @Param("ay") int ay,
                      @Param("arananGruplar") Collection<Long> arananGruplar,
                      @Param("kisitli") boolean kisitli, @Param("izinliGruplar") Collection<Long> izinliGruplar,
                      Pageable pageable);
}
