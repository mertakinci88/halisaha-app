package com.msc.halisaha.repository;

import com.msc.halisaha.entity.Urun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UrunRepository extends JpaRepository<Urun, Long> {

    /**
     * Stoğu tek SQL ile atomik düşer; stok yetersizse 0 satır etkilenir. İki kişi aynı anda son ürünü
     * satmaya çalışsa da biri başarılı olur, stok eksiye düşmez.
     * (clearAutomatically kullanılmaz: aynı transaction'daki rezervasyon entity'si detach olmasın.)
     */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Urun u SET u.adet = u.adet - :miktar WHERE u.id = :id AND u.adet >= :miktar")
    int stokDus(@Param("id") Long id, @Param("miktar") int miktar);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE Urun u SET u.adet = u.adet + :miktar WHERE u.id = :id")
    int stokEkle(@Param("id") Long id, @Param("miktar") int miktar);

    /** Veritabanındaki güncel adet (persistence context'teki eski değeri değil). */
    @Query("SELECT u.adet FROM Urun u WHERE u.id = :id")
    Integer mevcutAdet(@Param("id") Long id);
}
