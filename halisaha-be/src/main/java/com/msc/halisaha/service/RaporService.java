package com.msc.halisaha.service;

import com.msc.halisaha.common.AppZaman;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.dto.CiroRaporu;
import com.msc.halisaha.dto.CiroSatir;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RaporService {

    public enum Esas {
        /** Paranın kasaya girdiği tarihe göre (tahsilat). */
        ODEME,
        /** Hizmetin verildiği seans tarihine göre (iptal edilmeyen rezervasyonların kalemleri). */
        SEANS
    }

    public enum Grupla {
        GUN("day"),
        HAFTA("week"),
        AY("month");

        private final String sqlBirim;

        Grupla(String sqlBirim) {
            this.sqlBirim = sqlBirim;
        }
    }

    private static final long MAX_GUN = 366;

    /**
     * İşletme günü 12:00'da başlayıp ertesi gün 02:00'de biter; 00:00-02:00 arası kayıtlar
     * bir önceki güne yazılsın diye zaman 2 saat geri kaydırılarak gruplanır.
     */
    private static final String ISLETME_GUNU = "interval '2 hours'";

    private final NamedParameterJdbcTemplate jdbc;

    public CiroRaporu ciro(LocalDate baslangic, LocalDate bitis, Long sahaId, Grupla grupla, Esas esas) {
        LocalDate son = bitis != null ? bitis : AppZaman.bugun();
        LocalDate ilk = baslangic != null ? baslangic : son.minusDays(29);
        if (son.isBefore(ilk)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bitiş tarihi başlangıç tarihinden önce olamaz");
        }
        if (ChronoUnit.DAYS.between(ilk, son) > MAX_GUN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Rapor aralığı en fazla " + MAX_GUN + " gün olabilir");
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("bas", ilk.atStartOfDay())
                .addValue("bit", son.plusDays(1).atStartOfDay());
        String sahaFiltre = "";
        if (sahaId != null) {
            sahaFiltre = " AND r.saha_id = :sahaId";
            params.addValue("sahaId", sahaId);
        }

        List<CiroSatir> satirlar = esas == Esas.ODEME
                ? tahsilat(grupla, sahaFiltre, params)
                : seans(grupla, sahaFiltre, params);

        BigDecimal toplam = satirlar.stream().map(CiroSatir::toplam).reduce(BigDecimal.ZERO, BigDecimal::add);
        long adet = satirlar.stream().mapToLong(CiroSatir::adet).sum();
        return new CiroRaporu(esas.name(), grupla.name(), ilk, son, sahaId, toplam, adet, satirlar);
    }

    private List<CiroSatir> tahsilat(Grupla grupla, String sahaFiltre, MapSqlParameterSource params) {
        String sql = """
                SELECT date_trunc('%s', o.odeme_tarihi - %s)::date AS donem,
                       SUM(o.tutar) AS toplam,
                       COALESCE(SUM(o.tutar) FILTER (WHERE o.yontem = 'NAKIT'), 0) AS nakit,
                       COALESCE(SUM(o.tutar) FILTER (WHERE o.yontem = 'KART'), 0) AS kart,
                       COALESCE(SUM(o.tutar) FILTER (WHERE o.yontem = 'HAVALE_EFT'), 0) AS havale_eft,
                       COUNT(*) AS adet
                FROM odeme o
                JOIN rezervasyon r ON r.id = o.rezervasyon_id
                WHERE o.odeme_tarihi - %s >= :bas AND o.odeme_tarihi - %s < :bit%s
                GROUP BY 1
                ORDER BY 1
                """.formatted(grupla.sqlBirim, ISLETME_GUNU, ISLETME_GUNU, ISLETME_GUNU, sahaFiltre);

        return jdbc.query(sql, params, (rs, i) -> new CiroSatir(
                rs.getDate("donem").toLocalDate(),
                rs.getBigDecimal("toplam"),
                rs.getBigDecimal("nakit"),
                rs.getBigDecimal("kart"),
                rs.getBigDecimal("havale_eft"),
                null,
                null,
                rs.getLong("adet")));
    }

    private List<CiroSatir> seans(Grupla grupla, String sahaFiltre, MapSqlParameterSource params) {
        String sql = """
                SELECT date_trunc('%s', r.baslangic_tarih - %s)::date AS donem,
                       COALESCE(SUM(k.tutar) FILTER (WHERE k.tip = 'SAHA'), 0) AS saha,
                       COALESCE(SUM(k.tutar) FILTER (WHERE k.tip = 'URUN'), 0) AS urun,
                       COUNT(DISTINCT r.id) AS adet
                FROM rezervasyon r
                JOIN rezervasyon_kalem k ON k.rezervasyon_id = r.id
                WHERE r.durum = 'AKTIF'
                  AND r.baslangic_tarih - %s >= :bas AND r.baslangic_tarih - %s < :bit%s
                GROUP BY 1
                ORDER BY 1
                """.formatted(grupla.sqlBirim, ISLETME_GUNU, ISLETME_GUNU, ISLETME_GUNU, sahaFiltre);

        return jdbc.query(sql, params, (rs, i) -> {
            BigDecimal saha = rs.getBigDecimal("saha");
            BigDecimal urun = rs.getBigDecimal("urun");
            return new CiroSatir(
                    rs.getDate("donem").toLocalDate(),
                    saha.add(urun),
                    null,
                    null,
                    null,
                    saha,
                    urun,
                    rs.getLong("adet"));
        });
    }
}
