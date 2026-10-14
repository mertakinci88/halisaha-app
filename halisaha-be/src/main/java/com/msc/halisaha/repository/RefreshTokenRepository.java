package com.msc.halisaha.repository;

import com.msc.halisaha.entity.RefreshToken;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    /** Şifre değişince kullanıcının açık oturumları yenilenemesin diye refresh token'larını iptal eder. */
    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.kullanici.id = :kullaniciId AND t.revoked = false")
    int tumunuIptalEt(@Param("kullaniciId") Long kullaniciId);

    /** Süresi dolmuş ama çıkış yapılmadığı için hâlâ iptal edilmemiş oturumlar. */
    @Query("SELECT t FROM RefreshToken t JOIN FETCH t.kullanici k LEFT JOIN FETCH k.isletme WHERE t.revoked = false AND t.expiryDate < :simdi ORDER BY t.expiryDate")
    List<RefreshToken> suresiDolanlar(@Param("simdi") Instant simdi, Pageable sayfa);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM RefreshToken t WHERE t.expiryDate < :sinir")
    int eskileriSil(@Param("sinir") Instant sinir);
}
