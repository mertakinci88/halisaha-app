package com.msc.halisaha.service;

import com.msc.halisaha.common.security.IsletmeContext;
import com.msc.halisaha.dto.RezervasyonRaporu;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.msc.halisaha.service.OgrenciRaporService.oran;
import static com.msc.halisaha.service.RaporAraligi.GUNLUK_SAAT;
import static com.msc.halisaha.service.RaporAraligi.ISLETME_GUNU;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RezervasyonRaporService {

    private static final int ACILIS_SAATI = 12;

    private final NamedParameterJdbcTemplate jdbc;
    private final IsletmeContext isletmeContext;

    public RezervasyonRaporu getir(LocalDate baslangic, LocalDate bitis) {
        RaporAraligi aralik = RaporAraligi.of(baslangic, bitis);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("isletmeId", isletmeContext.id())
                .addValue("bas", aralik.bas())
                .addValue("bit", aralik.bit());
        BigDecimal kapasite = BigDecimal.valueOf(aralik.gunSayisi() * GUNLUK_SAAT);

        List<RezervasyonRaporu.SahaSatiri> sahalar = jdbc.query("""
                SELECT s.id, s.ad,
                       COUNT(r.id) FILTER (WHERE r.durum = 'AKTIF') AS aktif,
                       COUNT(r.id) FILTER (WHERE r.durum = 'IPTAL') AS iptal,
                       COALESCE(SUM(EXTRACT(EPOCH FROM (r.bitis_tarih - r.baslangic_tarih)) / 3600)
                                FILTER (WHERE r.durum = 'AKTIF'), 0) AS saat,
                       COALESCE(SUM(g.tutar) FILTER (WHERE r.durum = 'AKTIF'), 0) AS gelir
                FROM saha s
                LEFT JOIN rezervasyon r ON r.saha_id = s.id
                     AND r.baslangic_tarih >= (CAST(:bas AS timestamp) + %1$s) AND r.baslangic_tarih < (CAST(:bit AS timestamp) + %1$s)
                LEFT JOIN LATERAL (SELECT SUM(k.tutar) AS tutar FROM rezervasyon_kalem k
                                   WHERE k.rezervasyon_id = r.id AND k.tip = 'SAHA') g ON TRUE
                WHERE s.isletme_id = :isletmeId
                GROUP BY s.id, s.ad
                ORDER BY s.ad
                """.formatted(ISLETME_GUNU), params, (rs, i) -> {
            BigDecimal saat = rs.getBigDecimal("saat").setScale(1, RoundingMode.HALF_UP);
            return new RezervasyonRaporu.SahaSatiri(rs.getLong("id"), rs.getString("ad"), rs.getLong("aktif"),
                    rs.getLong("iptal"), saat, rs.getBigDecimal("gelir"), yuzde(saat, kapasite));
        });

        long rezervasyon = sahalar.stream().mapToLong(RezervasyonRaporu.SahaSatiri::rezervasyon).sum();
        long iptal = sahalar.stream().mapToLong(RezervasyonRaporu.SahaSatiri::iptal).sum();
        BigDecimal toplamSaat = sahalar.stream().map(RezervasyonRaporu.SahaSatiri::saat).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal toplamKapasite = kapasite.multiply(BigDecimal.valueOf(sahalar.size()));

        return new RezervasyonRaporu(aralik.ilk(), aralik.son(), rezervasyon, iptal, oran(iptal, rezervasyon + iptal),
                toplamSaat, yuzde(toplamSaat, toplamKapasite), sahalar, saatler(params), gunler(params));
    }

    /** Başlangıç saatine göre aktif rezervasyon sayısı; 00:00 ve 01:00 önceki günün devamı olduğundan 24 ve 25 gösterilir. */
    private List<RezervasyonRaporu.SaatSatiri> saatler(MapSqlParameterSource params) {
        Map<Integer, Long> sayilar = new TreeMap<>();
        for (int s = ACILIS_SAATI; s < ACILIS_SAATI + GUNLUK_SAAT; s++) sayilar.put(s, 0L);
        jdbc.query("""
                SELECT EXTRACT(HOUR FROM r.baslangic_tarih)::int AS saat, COUNT(*) AS adet
                FROM rezervasyon r
                WHERE r.isletme_id = :isletmeId AND r.durum = 'AKTIF'
                  AND r.baslangic_tarih >= (CAST(:bas AS timestamp) + %1$s) AND r.baslangic_tarih < (CAST(:bit AS timestamp) + %1$s)
                GROUP BY 1
                """.formatted(ISLETME_GUNU), params, rs -> {
            int saat = rs.getInt("saat");
            sayilar.merge(saat < ACILIS_SAATI ? saat + 24 : saat, rs.getLong("adet"), Long::sum);
        });
        List<RezervasyonRaporu.SaatSatiri> sonuc = new ArrayList<>();
        sayilar.forEach((saat, adet) -> sonuc.add(new RezervasyonRaporu.SaatSatiri(saat, adet)));
        return sonuc;
    }

    private List<RezervasyonRaporu.GunSatiri> gunler(MapSqlParameterSource params) {
        Map<Integer, Long> sayilar = new TreeMap<>();
        for (int g = 1; g <= 7; g++) sayilar.put(g, 0L);
        jdbc.query("""
                SELECT EXTRACT(ISODOW FROM r.baslangic_tarih - %1$s)::int AS gun, COUNT(*) AS adet
                FROM rezervasyon r
                WHERE r.isletme_id = :isletmeId AND r.durum = 'AKTIF'
                  AND r.baslangic_tarih >= (CAST(:bas AS timestamp) + %1$s) AND r.baslangic_tarih < (CAST(:bit AS timestamp) + %1$s)
                GROUP BY 1
                """.formatted(ISLETME_GUNU), params, rs -> {
            sayilar.put(rs.getInt("gun"), rs.getLong("adet"));
        });
        List<RezervasyonRaporu.GunSatiri> sonuc = new ArrayList<>();
        sayilar.forEach((gun, adet) -> sonuc.add(new RezervasyonRaporu.GunSatiri(gun, adet)));
        return sonuc;
    }

    private BigDecimal yuzde(BigDecimal pay, BigDecimal payda) {
        if (payda.signum() == 0) return BigDecimal.ZERO;
        return pay.multiply(BigDecimal.valueOf(100)).divide(payda, 1, RoundingMode.HALF_UP);
    }
}
