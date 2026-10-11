package com.msc.halisaha.repository;

import com.msc.halisaha.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    /** Şifre değişince kullanıcının açık oturumları yenilenemesin diye refresh token'larını iptal eder. */
    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.kullanici.id = :kullaniciId AND t.revoked = false")
    int tumunuIptalEt(@Param("kullaniciId") Long kullaniciId);
}
