package com.msc.halisaha.service;

import com.msc.halisaha.common.AppZaman;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.common.security.IsletmeContext;
import com.msc.halisaha.dto.OgrenciAylikRaporu;
import com.msc.halisaha.dto.OgrenciRaporu;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OgrenciRaporService {

    private static final int TREND_AY = 12;

    private final NamedParameterJdbcTemplate jdbc;
    private final IsletmeContext isletmeContext;

    public OgrenciRaporu getir(Integer yil, Integer ay) {
        YearMonth bugun = YearMonth.from(AppZaman.bugun());
        YearMonth donem;
        try {
            donem = (yil != null && ay != null) ? YearMonth.of(yil, ay) : bugun;
        } catch (java.time.DateTimeException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Geçersiz yıl/ay");
        }
        YearMonth onceki = donem.minusMonths(1);
        Long isletmeId = isletmeContext.id();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("isletmeId", isletmeId)
                .addValue("yil", donem.getYear())
                .addValue("ay", donem.getMonthValue())
                .addValue("oncekiYil", onceki.getYear())
                .addValue("oncekiAy", onceki.getMonthValue())
                .addValue("ayBasi", donem.atDay(1).atStartOfDay())
                .addValue("aySonu", donem.plusMonths(1).atDay(1).atStartOfDay())
                .addValue("oncekiAyBasi", onceki.atDay(1).atStartOfDay());

        // Ödemesiz (ödeme alınmayan) öğrenciler tahsilat hesabının dışındadır; ayrıca sayılır
        long aktif = sayi("SELECT COUNT(*) FROM ogrenci WHERE isletme_id = :isletmeId AND durum = 'AKTIF' AND odeme_alinir", params);
        long odemesiz = sayi("SELECT COUNT(*) FROM ogrenci WHERE isletme_id = :isletmeId AND durum = 'AKTIF' AND NOT odeme_alinir", params);
        long pasif = sayi("SELECT COUNT(*) FROM ogrenci WHERE isletme_id = :isletmeId AND durum = 'PASIF'", params);
        long odeyen = sayi("""
                SELECT COUNT(*) FROM ogrenci o
                WHERE o.isletme_id = :isletmeId AND o.durum = 'AKTIF' AND o.odeme_alinir AND EXISTS (
                    SELECT 1 FROM ogrenci_odeme p
                    WHERE p.ogrenci_id = o.id AND p.yil = :yil AND p.ay = :ay AND p.tamam)
                """, params);
        long kismi = sayi("""
                SELECT COUNT(*) FROM ogrenci o
                WHERE o.isletme_id = :isletmeId AND o.durum = 'AKTIF' AND o.odeme_alinir
                  AND EXISTS (SELECT 1 FROM ogrenci_odeme p
                              WHERE p.ogrenci_id = o.id AND p.yil = :yil AND p.ay = :ay AND p.odenen_tutar > 0)
                  AND NOT EXISTS (SELECT 1 FROM ogrenci_odeme p
                                  WHERE p.ogrenci_id = o.id AND p.yil = :yil AND p.ay = :ay AND p.tamam)
                """, params);
        long ardisik = sayi("""
                SELECT COUNT(*) FROM ogrenci o
                WHERE o.isletme_id = :isletmeId AND o.durum = 'AKTIF' AND o.odeme_alinir AND o.kayit_tarih < :oncekiAyBasi
                  AND NOT EXISTS (
                    SELECT 1 FROM ogrenci_odeme p
                    WHERE p.ogrenci_id = o.id AND p.tamam
                      AND ((p.yil = :yil AND p.ay = :ay) OR (p.yil = :oncekiYil AND p.ay = :oncekiAy)))
                """, params);
        long yeni = sayi("""
                SELECT COUNT(*) FROM ogrenci
                WHERE isletme_id = :isletmeId AND kayit_tarih >= :ayBasi AND kayit_tarih < :aySonu
                """, params);

        return new OgrenciRaporu(donem.getYear(), donem.getMonthValue(), aktif, pasif, odeyen, aktif - odeyen, kismi,
                oran(odeyen, aktif), ardisik, yeni, odemesiz, gruplar(params), trend(donem, params));
    }

    private List<OgrenciRaporu.GrupSatiri> gruplar(MapSqlParameterSource params) {
        // Tutar, o ay grubun öğrencilerinden alınan tüm ödemelerdir (ödemesiz ya da pasif olanların kayıtları dahil)
        return jdbc.query("""
                SELECT grup_id, grup_ad, COUNT(*) FILTER (WHERE sayilir) AS aktif,
                       COUNT(*) FILTER (WHERE sayilir AND odedi) AS odeyen, COALESCE(SUM(tutar), 0) AS tutar
                FROM (
                    SELECT gr.id AS grup_id, COALESCE(gr.ad, 'Grupsuz') AS grup_ad,
                           (o.durum = 'AKTIF' AND o.odeme_alinir) AS sayilir,
                           EXISTS (SELECT 1 FROM ogrenci_odeme p
                                   WHERE p.ogrenci_id = o.id AND p.yil = :yil AND p.ay = :ay AND p.tamam) AS odedi,
                           (SELECT SUM(p.odenen_tutar) FROM ogrenci_odeme p
                            WHERE p.ogrenci_id = o.id AND p.yil = :yil AND p.ay = :ay) AS tutar
                    FROM ogrenci o LEFT JOIN grup gr ON gr.id = o.grup_id
                    WHERE o.isletme_id = :isletmeId
                ) t
                GROUP BY grup_id, grup_ad
                HAVING COUNT(*) FILTER (WHERE sayilir) > 0 OR COALESCE(SUM(tutar), 0) > 0
                ORDER BY grup_ad
                """, params, (rs, i) -> {
            long grupAktif = rs.getLong("aktif");
            long grupOdeyen = rs.getLong("odeyen");
            long id = rs.getLong("grup_id");
            return new OgrenciRaporu.GrupSatiri(rs.wasNull() ? null : id, rs.getString("grup_ad"), grupAktif, grupOdeyen,
                    oran(grupOdeyen, grupAktif), rs.getBigDecimal("tutar"));
        });
    }

    /** Seçilen ayda biten son 12 ay; ödeme olmayan aylar sıfır döner. */
    private List<OgrenciRaporu.AySatiri> trend(YearMonth donem, MapSqlParameterSource params) {
        YearMonth ilk = donem.minusMonths(TREND_AY - 1L);
        MapSqlParameterSource p = new MapSqlParameterSource(params.getValues())
                .addValue("ilkYil", ilk.getYear()).addValue("ilkAy", ilk.getMonthValue())
                .addValue("sonYil", donem.getYear()).addValue("sonAy", donem.getMonthValue());
        Map<YearMonth, OgrenciRaporu.AySatiri> bulunan = new HashMap<>();
        jdbc.query("""
                SELECT p.yil, p.ay, COUNT(DISTINCT p.ogrenci_id) FILTER (WHERE p.tamam) AS odeyen, SUM(p.odenen_tutar) AS tutar
                FROM ogrenci_odeme p
                WHERE p.isletme_id = :isletmeId AND (p.odenen_tutar > 0 OR p.tamam)
                  AND (p.yil, p.ay) >= (:ilkYil, :ilkAy) AND (p.yil, p.ay) <= (:sonYil, :sonAy)
                GROUP BY p.yil, p.ay
                """, p, rs -> {
            bulunan.put(YearMonth.of(rs.getInt("yil"), rs.getInt("ay")),
                    new OgrenciRaporu.AySatiri(rs.getInt("yil"), rs.getInt("ay"), rs.getLong("odeyen"), rs.getBigDecimal("tutar")));
        });
        List<OgrenciRaporu.AySatiri> sonuc = new ArrayList<>();
        for (YearMonth m = ilk; !m.isAfter(donem); m = m.plusMonths(1)) {
            sonuc.add(bulunan.getOrDefault(m, new OgrenciRaporu.AySatiri(m.getYear(), m.getMonthValue(), 0, BigDecimal.ZERO)));
        }
        return sonuc;
    }

    /** aktif: aktif ve ödemesi alınan öğrenci (ödemesizler ödeme oranlarına girmez); yeniKayit için tüm öğrenciler kullanılır. */
    private record OgrenciKaydi(long id, Long grupId, String grupAd, boolean aktif, LocalDateTime kayit) {
    }

    /** Birden fazla aya yayılan dönem: her ayın ödeme tahsilatı ve grup bazında özet. */
    public OgrenciAylikRaporu aylik(LocalDate baslangic, LocalDate bitis) {
        RaporAraligi aralik = RaporAraligi.of(baslangic, bitis);
        Long isletmeId = isletmeContext.id();
        YearMonth ilkAy = YearMonth.from(aralik.ilk());
        YearMonth sonAy = YearMonth.from(aralik.son());
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("isletmeId", isletmeId)
                .addValue("ilkYil", ilkAy.getYear()).addValue("ilkAy", ilkAy.getMonthValue())
                .addValue("sonYil", sonAy.getYear()).addValue("sonAy", sonAy.getMonthValue());

        Map<Long, OgrenciKaydi> ogrenciler = new HashMap<>();
        jdbc.query("""
                SELECT o.id, o.grup_id, gr.ad AS grup_ad, o.durum, o.odeme_alinir, o.kayit_tarih
                FROM ogrenci o LEFT JOIN grup gr ON gr.id = o.grup_id
                WHERE o.isletme_id = :isletmeId
                """, params, rs -> {
            long grupId = rs.getLong("grup_id");
            Long grup = rs.wasNull() ? null : grupId;
            ogrenciler.put(rs.getLong("id"), new OgrenciKaydi(rs.getLong("id"), grup,
                    grup == null ? "Grupsuz" : rs.getString("grup_ad"), "AKTIF".equals(rs.getString("durum")) && rs.getBoolean("odeme_alinir"),
                    rs.getTimestamp("kayit_tarih").toLocalDateTime()));
        });

        // Ay -> o ay ödeme yapan öğrenciler ve ayın toplam tutarı
        Map<YearMonth, Set<Long>> odeyenler = new HashMap<>();
        Map<YearMonth, Set<Long>> odemeAlanlar = new HashMap<>();   // tutarı olan ödemesi girilmiş (tamam olsun olmasın)
        Map<YearMonth, BigDecimal> tutarlar = new HashMap<>();
        Map<Long, BigDecimal> grupTutarlari = new HashMap<>();
        jdbc.query("""
                SELECT p.yil, p.ay, p.ogrenci_id, p.odenen_tutar, p.tamam
                FROM ogrenci_odeme p
                WHERE p.isletme_id = :isletmeId AND (p.odenen_tutar > 0 OR p.tamam)
                  AND (p.yil, p.ay) >= (:ilkYil, :ilkAy) AND (p.yil, p.ay) <= (:sonYil, :sonAy)
                """, params, rs -> {
            YearMonth ay = YearMonth.of(rs.getInt("yil"), rs.getInt("ay"));
            long ogrenciId = rs.getLong("ogrenci_id");
            BigDecimal tutar = rs.getBigDecimal("odenen_tutar");
            if (tutar.signum() > 0) {
                odemeAlanlar.computeIfAbsent(ay, k -> new HashSet<>()).add(ogrenciId);
            }
            if (rs.getBoolean("tamam")) {
                odeyenler.computeIfAbsent(ay, k -> new HashSet<>()).add(ogrenciId);
            }
            tutarlar.merge(ay, tutar, BigDecimal::add);
            OgrenciKaydi kayit = ogrenciler.get(ogrenciId);
            grupTutarlari.merge(kayit != null && kayit.grupId() != null ? kayit.grupId() : -1L, tutar, BigDecimal::add);
        });

        List<OgrenciAylikRaporu.AySatiri> aylar = new ArrayList<>();
        // Grup -> ay bazında (ödeyen / uygun) oranlarının toplamı ve oranı hesaplanan ay sayısı
        Map<Long, BigDecimal> grupOranToplami = new HashMap<>();
        Map<Long, Integer> grupOranAyi = new HashMap<>();
        Map<Long, String> grupAdlari = new LinkedHashMap<>();
        BigDecimal oranToplami = BigDecimal.ZERO;
        int oranAyi = 0;
        BigDecimal toplamTutar = BigDecimal.ZERO;
        int ayAdedi = 0;

        for (YearMonth ay = ilkAy; !ay.isAfter(sonAy); ay = ay.plusMonths(1)) {
            LocalDateTime aySonu = ay.plusMonths(1).atDay(1).atStartOfDay();
            Set<Long> odeyen = odeyenler.getOrDefault(ay, Set.of());
            Set<Long> odemeAlan = odemeAlanlar.getOrDefault(ay, Set.of());
            long kismiSayisi = 0;
            long uygun = 0;
            long odeyenSayisi = 0;
            Map<Long, long[]> grupSayilari = new HashMap<>();   // grup -> {uygun, ödeyen}
            for (OgrenciKaydi o : ogrenciler.values()) {
                if (!o.aktif() || !o.kayit().isBefore(aySonu)) continue;
                long grupAnahtari = o.grupId() != null ? o.grupId() : -1L;
                grupAdlari.putIfAbsent(grupAnahtari, o.grupAd());
                long[] sayi = grupSayilari.computeIfAbsent(grupAnahtari, k -> new long[2]);
                sayi[0]++;
                uygun++;
                if (odeyen.contains(o.id())) {
                    sayi[1]++;
                    odeyenSayisi++;
                } else if (odemeAlan.contains(o.id())) {
                    kismiSayisi++;
                }
            }
            BigDecimal oran = oran(odeyenSayisi, uygun);
            BigDecimal tutar = tutarlar.getOrDefault(ay, BigDecimal.ZERO);
            aylar.add(new OgrenciAylikRaporu.AySatiri(ay.getYear(), ay.getMonthValue(), uygun, odeyenSayisi, uygun - odeyenSayisi, kismiSayisi, oran, tutar));
            toplamTutar = toplamTutar.add(tutar);
            ayAdedi++;
            if (uygun > 0) {
                oranToplami = oranToplami.add(oran);
                oranAyi++;
            }
            grupSayilari.forEach((grup, sayi) -> {
                grupOranToplami.merge(grup, oran(sayi[1], sayi[0]), BigDecimal::add);
                grupOranAyi.merge(grup, 1, Integer::sum);
            });
        }

        long aktif = ogrenciler.values().stream().filter(OgrenciKaydi::aktif).count();
        long yeni = ogrenciler.values().stream()
                .filter(o -> !o.kayit().isBefore(aralik.bas()) && o.kayit().isBefore(aralik.bit())).count();

        Map<Long, Long> grupAktifleri = new HashMap<>();
        ogrenciler.values().stream().filter(OgrenciKaydi::aktif).forEach(o -> {
            long anahtar = o.grupId() != null ? o.grupId() : -1L;
            grupAktifleri.merge(anahtar, 1L, Long::sum);
            grupAdlari.putIfAbsent(anahtar, o.grupAd());
        });
        List<OgrenciAylikRaporu.GrupSatiri> gruplar = new ArrayList<>();
        grupAdlari.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(e -> {
                    long grup = e.getKey();
                    int ayi = grupOranAyi.getOrDefault(grup, 0);
                    BigDecimal ortalama = ayi == 0 ? BigDecimal.ZERO
                            : grupOranToplami.get(grup).divide(BigDecimal.valueOf(ayi), 1, RoundingMode.HALF_UP);
                    gruplar.add(new OgrenciAylikRaporu.GrupSatiri(grup == -1L ? null : grup, e.getValue(),
                            grupAktifleri.getOrDefault(grup, 0L), grupTutarlari.getOrDefault(grup, BigDecimal.ZERO), ortalama));
                });

        BigDecimal ortalamaOran = oranAyi == 0 ? BigDecimal.ZERO
                : oranToplami.divide(BigDecimal.valueOf(oranAyi), 1, RoundingMode.HALF_UP);
        BigDecimal aylikOrtalama = ayAdedi == 0 ? BigDecimal.ZERO
                : toplamTutar.divide(BigDecimal.valueOf(ayAdedi), 2, RoundingMode.HALF_UP);
        return new OgrenciAylikRaporu(aralik.ilk(), aralik.son(), aktif, yeni, toplamTutar, ortalamaOran, aylikOrtalama, aylar, gruplar);
    }

    private long sayi(String sql, MapSqlParameterSource params) {
        Long deger = jdbc.queryForObject(sql, params, Long.class);
        return deger != null ? deger : 0;
    }

    static BigDecimal oran(long pay, long payda) {
        if (payda == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(pay * 100).divide(BigDecimal.valueOf(payda), 1, RoundingMode.HALF_UP);
    }
}
