package com.msc.halisaha.service;

import com.msc.halisaha.common.security.IsletmeContext;
import com.msc.halisaha.dto.YoklamaRaporu;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static com.msc.halisaha.service.OgrenciRaporService.oran;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class YoklamaRaporService {

    private static final int DEVAMSIZ_LISTE = 10;

    private final NamedParameterJdbcTemplate jdbc;
    private final IsletmeContext isletmeContext;

    public YoklamaRaporu getir(LocalDate baslangic, LocalDate bitis) {
        RaporAraligi aralik = RaporAraligi.of(baslangic, bitis);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("isletmeId", isletmeContext.id())
                .addValue("ilk", aralik.ilk())
                .addValue("son", aralik.son())
                .addValue("limit", DEVAMSIZ_LISTE);

        long[] toplam = jdbc.queryForObject("""
                SELECT COUNT(DISTINCT tarih) AS gun,
                       COUNT(*) FILTER (WHERE durum = 'GELDI') AS geldi,
                       COUNT(*) FILTER (WHERE durum = 'GELMEDI') AS gelmedi,
                       COUNT(*) FILTER (WHERE durum = 'IZINLI') AS izinli
                FROM yoklama WHERE isletme_id = :isletmeId AND tarih BETWEEN :ilk AND :son
                """, params, (rs, i) -> new long[]{rs.getLong("gun"), rs.getLong("geldi"), rs.getLong("gelmedi"), rs.getLong("izinli")});

        List<YoklamaRaporu.GrupSatiri> gruplar = jdbc.query("""
                SELECT gr.id AS grup_id, COALESCE(gr.ad, 'Grupsuz') AS grup_ad,
                       COUNT(*) FILTER (WHERE y.durum = 'GELDI') AS geldi,
                       COUNT(*) FILTER (WHERE y.durum = 'GELMEDI') AS gelmedi,
                       COUNT(*) FILTER (WHERE y.durum = 'IZINLI') AS izinli
                FROM yoklama y JOIN ogrenci o ON o.id = y.ogrenci_id LEFT JOIN grup gr ON gr.id = o.grup_id
                WHERE y.isletme_id = :isletmeId AND y.tarih BETWEEN :ilk AND :son
                GROUP BY gr.id, gr.ad
                ORDER BY COALESCE(gr.ad, 'Grupsuz')
                """, params, (rs, i) -> {
            long id = rs.getLong("grup_id");
            boolean grupsuz = rs.wasNull();
            long geldi = rs.getLong("geldi");
            long gelmedi = rs.getLong("gelmedi");
            return new YoklamaRaporu.GrupSatiri(grupsuz ? null : id, rs.getString("grup_ad"), geldi, gelmedi,
                    rs.getLong("izinli"), oran(geldi, geldi + gelmedi));
        });

        List<YoklamaRaporu.GunSatiri> gunler = jdbc.query("""
                SELECT EXTRACT(ISODOW FROM tarih)::int AS gun,
                       COUNT(*) FILTER (WHERE durum = 'GELDI') AS geldi,
                       COUNT(*) FILTER (WHERE durum = 'GELMEDI') AS gelmedi,
                       COUNT(*) FILTER (WHERE durum = 'IZINLI') AS izinli
                FROM yoklama WHERE isletme_id = :isletmeId AND tarih BETWEEN :ilk AND :son
                GROUP BY 1 ORDER BY 1
                """, params, (rs, i) -> new YoklamaRaporu.GunSatiri(rs.getInt("gun"), rs.getLong("geldi"),
                rs.getLong("gelmedi"), rs.getLong("izinli")));

        // En çok devamsızlık yapan aktif öğrenciler
        List<YoklamaRaporu.OgrenciSatiri> devamsizlar = jdbc.query("""
                SELECT o.id, o.ad_soyad, gr.ad AS grup_ad,
                       COUNT(*) FILTER (WHERE y.durum = 'GELDI') AS geldi,
                       COUNT(*) FILTER (WHERE y.durum = 'GELMEDI') AS gelmedi
                FROM yoklama y JOIN ogrenci o ON o.id = y.ogrenci_id LEFT JOIN grup gr ON gr.id = o.grup_id
                WHERE y.isletme_id = :isletmeId AND y.tarih BETWEEN :ilk AND :son AND o.durum = 'AKTIF'
                GROUP BY o.id, o.ad_soyad, gr.ad
                HAVING COUNT(*) FILTER (WHERE y.durum = 'GELMEDI') > 0
                ORDER BY COUNT(*) FILTER (WHERE y.durum = 'GELMEDI') DESC, o.ad_soyad
                LIMIT :limit
                """, params, (rs, i) -> {
            long geldi = rs.getLong("geldi");
            long gelmedi = rs.getLong("gelmedi");
            return new YoklamaRaporu.OgrenciSatiri(rs.getLong("id"), rs.getString("ad_soyad"), rs.getString("grup_ad"),
                    gelmedi, geldi + gelmedi, oran(geldi, geldi + gelmedi));
        });

        return new YoklamaRaporu(aralik.ilk(), aralik.son(), toplam[0], toplam[1], toplam[2], toplam[3],
                oran(toplam[1], toplam[1] + toplam[2]), gruplar, gunler, devamsizlar);
    }
}
