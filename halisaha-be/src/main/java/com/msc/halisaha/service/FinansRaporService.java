package com.msc.halisaha.service;

import com.msc.halisaha.common.security.IsletmeContext;
import com.msc.halisaha.dto.FinansRaporu;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.msc.halisaha.service.RaporAraligi.ISLETME_GUNU;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinansRaporService {

    private static final long GUNLUK_TREND_MAX_GUN = 62;

    private final NamedParameterJdbcTemplate jdbc;
    private final IsletmeContext isletmeContext;

    private record Tahsilat(String kaynak, LocalDate donem, String yontem, BigDecimal tutar) {
    }

    public FinansRaporu getir(LocalDate baslangic, LocalDate bitis) {
        RaporAraligi aralik = RaporAraligi.of(baslangic, bitis);
        RaporAraligi onceki = aralik.onceki();
        boolean gunluk = aralik.gunSayisi() <= GUNLUK_TREND_MAX_GUN;
        String birim = gunluk ? "day" : "month";
        Long isletmeId = isletmeContext.id();

        List<Tahsilat> satirlar = tahsilatlar(aralik, birim, isletmeId);
        List<Tahsilat> oncekiSatirlar = tahsilatlar(onceki, "month", isletmeId);

        return new FinansRaporu(
                aralik.ilk(), aralik.son(), gunluk ? "GUN" : "AY",
                kaynakToplami(satirlar), kaynakToplami(oncekiSatirlar), onceki.ilk(), onceki.son(),
                bekleyen(aralik, isletmeId), kafeteryaSatisi(aralik, isletmeId), yontem(satirlar), trend(aralik, gunluk, satirlar));
    }

    /** Dönemdeki tahsilatlar (hepsi ödeme tarihine göre): kaynak, dönem başlangıcı, ödeme yöntemi (yöntem kaydı öncesi envanter ödemelerinde boş) ve tutar. */
    private List<Tahsilat> tahsilatlar(RaporAraligi aralik, String birim, Long isletmeId) {
        String sql = """
                SELECT 'REZERVASYON' AS kaynak, date_trunc('%1$s', o.odeme_tarihi - %2$s)::date AS donem,
                       o.yontem AS yontem, SUM(o.tutar) AS tutar
                FROM rezervasyon_odeme o JOIN rezervasyon r ON r.id = o.rezervasyon_id
                WHERE r.isletme_id = :isletmeId AND o.odeme_tarihi - %2$s >= :bas AND o.odeme_tarihi - %2$s < :bit
                GROUP BY 2, 3
                UNION ALL
                SELECT 'OGRENCI', date_trunc('%1$s', p.odeme_tarihi)::date, p.odeme_sekli, SUM(p.odenen_tutar)
                FROM ogrenci_odeme p JOIN ogrenci g ON g.id = p.ogrenci_id
                WHERE g.isletme_id = :isletmeId AND p.odeme_tarihi >= :basTarih AND p.odeme_tarihi < :bitTarih
                GROUP BY 2, 3
                UNION ALL
                SELECT 'ENVANTER', date_trunc('%1$s', p.odeme_tarihi - %2$s)::date, p.odeme_sekli, SUM(p.tutar)
                FROM envanter_satis_odeme p JOIN envanter_satis s ON s.id = p.satis_id
                WHERE s.isletme_id = :isletmeId AND p.odeme_tarihi - %2$s >= :bas AND p.odeme_tarihi - %2$s < :bit
                GROUP BY 2, 3
                """.formatted(birim, ISLETME_GUNU);

        return jdbc.query(sql, params(aralik, isletmeId), (rs, i) -> new Tahsilat(
                rs.getString("kaynak"), rs.getDate("donem").toLocalDate(), rs.getString("yontem"), rs.getBigDecimal("tutar")));
    }

    /** Seansı dönemde olan aktif rezervasyonlara eklenen kafeterya ürünlerinin tutarı; ödenip ödenmediğine bakılmaz. */
    private BigDecimal kafeteryaSatisi(RaporAraligi aralik, Long isletmeId) {
        return jdbc.queryForObject("""
                SELECT COALESCE(SUM(k.tutar), 0)
                FROM rezervasyon_kalem k JOIN rezervasyon r ON r.id = k.rezervasyon_id
                WHERE r.isletme_id = :isletmeId AND r.durum = 'AKTIF' AND k.tip = 'URUN'
                  AND r.baslangic_tarih - %s >= :bas AND r.baslangic_tarih - %s < :bit
                """.formatted(ISLETME_GUNU, ISLETME_GUNU), params(aralik, isletmeId), BigDecimal.class);
    }

    private FinansRaporu.Bekleyen bekleyen(RaporAraligi aralik, Long isletmeId) {
        MapSqlParameterSource params = params(aralik, isletmeId);
        // Seansı dönemde olan aktif rezervasyonların kalem toplamı (saha + kafeterya ürünleri) - alınan ödemeler
        BigDecimal rezervasyon = jdbc.queryForObject("""
                SELECT COALESCE(SUM(GREATEST(k.toplam - COALESCE(od.odenen, 0), 0)), 0)
                FROM rezervasyon r
                JOIN (SELECT rezervasyon_id, SUM(tutar) AS toplam FROM rezervasyon_kalem GROUP BY rezervasyon_id) k
                  ON k.rezervasyon_id = r.id
                LEFT JOIN (SELECT rezervasyon_id, SUM(tutar) AS odenen FROM rezervasyon_odeme GROUP BY rezervasyon_id) od
                  ON od.rezervasyon_id = r.id
                WHERE r.isletme_id = :isletmeId AND r.durum = 'AKTIF'
                  AND r.baslangic_tarih - %s >= :bas AND r.baslangic_tarih - %s < :bit
                """.formatted(ISLETME_GUNU, ISLETME_GUNU), params, BigDecimal.class);
        BigDecimal envanter = jdbc.queryForObject("""
                SELECT COALESCE(SUM(s.set_tutari - s.odenen_tutar), 0)
                FROM envanter_satis s
                WHERE s.isletme_id = :isletmeId AND s.kayit_tarihi - %s >= :bas AND s.kayit_tarihi - %s < :bit
                """.formatted(ISLETME_GUNU, ISLETME_GUNU), params, BigDecimal.class);
        return new FinansRaporu.Bekleyen(rezervasyon, envanter, rezervasyon.add(envanter));
    }

    private FinansRaporu.Kaynak kaynakToplami(List<Tahsilat> satirlar) {
        BigDecimal rezervasyon = topla(satirlar, "REZERVASYON");
        BigDecimal ogrenci = topla(satirlar, "OGRENCI");
        BigDecimal envanter = topla(satirlar, "ENVANTER");
        return new FinansRaporu.Kaynak(rezervasyon, ogrenci, envanter, rezervasyon.add(ogrenci).add(envanter));
    }

    private BigDecimal topla(List<Tahsilat> satirlar, String kaynak) {
        return satirlar.stream().filter(t -> t.kaynak().equals(kaynak)).map(Tahsilat::tutar)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private FinansRaporu.Yontem yontem(List<Tahsilat> satirlar) {
        BigDecimal nakit = BigDecimal.ZERO;
        BigDecimal kart = BigDecimal.ZERO;
        BigDecimal havale = BigDecimal.ZERO;
        for (Tahsilat t : satirlar) {
            if ("NAKIT".equals(t.yontem())) nakit = nakit.add(t.tutar());
            else if ("KART".equals(t.yontem())) kart = kart.add(t.tutar());
            else if ("HAVALE_EFT".equals(t.yontem())) havale = havale.add(t.tutar());
        }
        return new FinansRaporu.Yontem(nakit, kart, havale);
    }

    /** Boş günleri/ayları sıfırla doldurur, böylece grafik kesintisiz olur. */
    private List<FinansRaporu.Nokta> trend(RaporAraligi aralik, boolean gunluk, List<Tahsilat> satirlar) {
        Map<LocalDate, BigDecimal[]> donemler = new TreeMap<>();
        LocalDate imlec = gunluk ? aralik.ilk() : aralik.ilk().withDayOfMonth(1);
        while (!imlec.isAfter(aralik.son())) {
            donemler.put(imlec, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            imlec = gunluk ? imlec.plusDays(1) : imlec.plusMonths(1);
        }
        for (Tahsilat t : satirlar) {
            BigDecimal[] hucre = donemler.get(t.donem());
            if (hucre == null) continue;
            int sira = switch (t.kaynak()) {
                case "REZERVASYON" -> 0;
                case "OGRENCI" -> 1;
                default -> 2;
            };
            hucre[sira] = hucre[sira].add(t.tutar());
        }
        List<FinansRaporu.Nokta> sonuc = new ArrayList<>();
        donemler.forEach((donem, h) -> sonuc.add(
                new FinansRaporu.Nokta(donem, h[0], h[1], h[2], h[0].add(h[1]).add(h[2]))));
        return sonuc;
    }

    private MapSqlParameterSource params(RaporAraligi aralik, Long isletmeId) {
        return new MapSqlParameterSource()
                .addValue("isletmeId", isletmeId)
                .addValue("bas", aralik.bas())
                .addValue("bit", aralik.bit())
                .addValue("basTarih", aralik.ilk())
                .addValue("bitTarih", aralik.son().plusDays(1));
    }
}
