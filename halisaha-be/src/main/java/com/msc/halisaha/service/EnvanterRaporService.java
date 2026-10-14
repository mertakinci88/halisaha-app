package com.msc.halisaha.service;

import com.msc.halisaha.common.security.IsletmeContext;
import com.msc.halisaha.dto.EnvanterRaporu;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.msc.halisaha.service.RaporAraligi.ISLETME_GUNU;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnvanterRaporService {

    private static final int LISTE_BOYUT = 10;

    private final NamedParameterJdbcTemplate jdbc;
    private final IsletmeContext isletmeContext;

    public EnvanterRaporu getir(LocalDate baslangic, LocalDate bitis) {
        RaporAraligi aralik = RaporAraligi.of(baslangic, bitis);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("isletmeId", isletmeContext.id())
                .addValue("bas", aralik.bas())
                .addValue("bit", aralik.bit())
                .addValue("limit", LISTE_BOYUT);

        BigDecimal[] ozet = jdbc.queryForObject("""
                SELECT COUNT(*) AS adet, COALESCE(SUM(set_tutari), 0) AS tutar, COALESCE(SUM(odenen_tutar), 0) AS odenen
                FROM envanter_satis
                WHERE isletme_id = :isletmeId AND kayit_tarihi - %1$s >= :bas AND kayit_tarihi - %1$s < :bit
                """.formatted(ISLETME_GUNU), params, (rs, i) -> new BigDecimal[]{
                BigDecimal.valueOf(rs.getLong("adet")), rs.getBigDecimal("tutar"), rs.getBigDecimal("odenen")});

        List<EnvanterRaporu.UrunSatiri> urunler = jdbc.query("""
                SELECT e.tur, e.beden, COUNT(*) AS adet, SUM(k.birim_fiyat) AS tutar
                FROM envanter_satis_kalem k
                JOIN envanter_satis s ON s.id = k.satis_id
                JOIN envanter e ON e.id = k.envanter_id
                WHERE s.isletme_id = :isletmeId AND s.kayit_tarihi - %1$s >= :bas AND s.kayit_tarihi - %1$s < :bit
                GROUP BY e.tur, e.beden
                ORDER BY adet DESC, tutar DESC
                LIMIT :limit
                """.formatted(ISLETME_GUNU), params, (rs, i) -> new EnvanterRaporu.UrunSatiri(
                rs.getString("tur"), rs.getString("beden"), rs.getLong("adet"), rs.getBigDecimal("tutar")));

        List<EnvanterRaporu.SetSatiri> setler = jdbc.query("""
                SELECT set_ad, COUNT(*) AS adet, SUM(set_tutari) AS tutar
                FROM envanter_satis
                WHERE isletme_id = :isletmeId AND kayit_tarihi - %1$s >= :bas AND kayit_tarihi - %1$s < :bit
                GROUP BY set_ad
                ORDER BY adet DESC, tutar DESC
                LIMIT :limit
                """.formatted(ISLETME_GUNU), params, (rs, i) -> new EnvanterRaporu.SetSatiri(
                rs.getString("set_ad"), rs.getLong("adet"), rs.getBigDecimal("tutar")));

        // Stok uyarı eşiğinin altına düşen kalemler (dönemden bağımsız, güncel durum)
        List<EnvanterRaporu.StokSatiri> kritik = jdbc.query("""
                SELECT tur, beden, adet, stok_uyari_adedi
                FROM envanter
                WHERE isletme_id = :isletmeId AND adet <= stok_uyari_adedi
                ORDER BY adet, tur, beden
                """, params, (rs, i) -> new EnvanterRaporu.StokSatiri(
                rs.getString("tur"), rs.getString("beden"), rs.getInt("adet"), rs.getInt("stok_uyari_adedi")));

        return new EnvanterRaporu(aralik.ilk(), aralik.son(), ozet[0].longValue(), ozet[1], ozet[2],
                ozet[1].subtract(ozet[2]), urunler, setler, kritik);
    }
}
