package com.msc.halisaha.service;

import com.msc.halisaha.common.security.IsletmeContext;
import com.msc.halisaha.dto.KullaniciRaporlari;
import com.msc.halisaha.dto.KullaniciRaporlari.GirisSatiri;
import com.msc.halisaha.dto.KullaniciRaporlari.IadeKaynakToplami;
import com.msc.halisaha.dto.KullaniciRaporlari.IadeSatiri;
import com.msc.halisaha.dto.KullaniciRaporlari.IptalKisi;
import com.msc.halisaha.dto.KullaniciRaporlari.IptalSatiri;
import com.msc.halisaha.dto.KullaniciRaporlari.IslemSatiri;
import com.msc.halisaha.dto.KullaniciRaporlari.KasaSatiri;
import com.msc.halisaha.dto.KullaniciRaporlari.YoklamaKullanici;
import com.msc.halisaha.dto.KullaniciRaporlari.YoklamaGunu;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.msc.halisaha.service.AbonelikService.ABONELIK_IPTALI;
import static com.msc.halisaha.service.RaporAraligi.ISLETME_GUNU;

/** Kullanıcı bazlı raporlar: kim ne kadar tahsilat aldı, ne iş yaptı, kim iade etti / iptal etti, yoklamayı kim aldı, kim ne zaman giriş yaptı. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KullaniciRaporService {

    /** Liste raporlarında en çok bu kadar satır döner; fazlası varsa "kesildi" işaretlenir. */
    private static final int SATIR_SINIRI = 500;

    private final NamedParameterJdbcTemplate jdbc;
    private final IsletmeContext isletmeContext;

    private record Kisi(Long id, String ad, String rol) {
    }

    private MapSqlParameterSource params(RaporAraligi aralik) {
        return new MapSqlParameterSource()
                .addValue("isletmeId", isletmeContext.id())
                .addValue("bas", aralik.bas())
                .addValue("bit", aralik.bit())
                .addValue("basTarih", aralik.ilk())
                .addValue("bitTarih", aralik.son().plusDays(1))
                .addValue("sonTarih", aralik.son())
                .addValue("abonelikIptali", ABONELIK_IPTALI);
    }

    /** İşletmenin tüm kullanıcıları; kullanıcı bilgisi tutulmamış kayıtlar için sonda null id'li "Belirtilmemiş" satırı. */
    private List<Kisi> kisiler(MapSqlParameterSource params) {
        List<Kisi> kisiler = new ArrayList<>(jdbc.query("""
                SELECT id, ad_soyad, rol FROM kullanici WHERE isletme_id = :isletmeId ORDER BY ad_soyad
                """, params, (rs, i) -> new Kisi(rs.getLong("id"), rs.getString("ad_soyad"), rs.getString("rol"))));
        kisiler.add(new Kisi(null, "Belirtilmemiş", null));
        return kisiler;
    }

    private <T> Map<Long, T> sorgu(String sql, MapSqlParameterSource params, Function<java.sql.ResultSet, T> okuyucu) {
        Map<Long, T> sonuc = new LinkedHashMap<>();
        jdbc.query(sql, params, rs -> {
            long id = rs.getLong(1);
            sonuc.put(rs.wasNull() ? null : id, okuyucu.apply(rs));
        });
        return sonuc;
    }

    private Map<Long, Long> sayilar(String sql, MapSqlParameterSource params) {
        return sorgu(sql, params, rs -> {
            try {
                return rs.getLong(2);
            } catch (java.sql.SQLException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    // ── 1) Kasa ──────────────────────────────────────────────────────────────────────────────

    public KullaniciRaporlari.Kasa kasa(LocalDate baslangic, LocalDate bitis) {
        RaporAraligi aralik = RaporAraligi.of(baslangic, bitis);
        MapSqlParameterSource params = params(aralik);
        String tarihAraligi = "(CAST(:bas AS timestamp) + " + ISLETME_GUNU + ")";
        String bitisAraligi = "(CAST(:bit AS timestamp) + " + ISLETME_GUNU + ")";

        // kullanıcı -> kaynak ve yönteme göre tutar
        record Hucre(Long kullanici, String kaynak, String yontem, Long hesap, BigDecimal tutar) {
        }
        List<Hucre> hucreler = jdbc.query("""
                SELECT kullanici_id, 'REZERVASYON' AS kaynak, yontem, hesap_id, SUM(tutar) AS tutar FROM rezervasyon_odeme
                WHERE isletme_id = :isletmeId AND odeme_tarihi >= %1$s AND odeme_tarihi < %2$s GROUP BY 1, 3, 4
                UNION ALL
                SELECT kullanici_id, 'OGRENCI', odeme_sekli, hesap_id, SUM(odenen_tutar) FROM ogrenci_odeme
                WHERE isletme_id = :isletmeId AND odeme_tarihi >= :basTarih AND odeme_tarihi < :bitTarih GROUP BY 1, 3, 4
                UNION ALL
                SELECT kullanici_id, 'ENVANTER', odeme_sekli, hesap_id, SUM(tutar) FROM envanter_satis_odeme
                WHERE isletme_id = :isletmeId AND odeme_tarihi >= %1$s AND odeme_tarihi < %2$s GROUP BY 1, 3, 4
                UNION ALL
                SELECT kullanici_id, 'ABONELIK', odeme_sekli, hesap_id, SUM(tutar) FROM abonelik_odeme
                WHERE isletme_id = :isletmeId AND odeme_tarihi >= :basTarih AND odeme_tarihi < :bitTarih GROUP BY 1, 3, 4
                UNION ALL
                SELECT kullanici_id, 'IADE', iade_sekli, hesap_id, SUM(tutar) FROM iade
                WHERE isletme_id = :isletmeId AND iade_tarihi >= :basTarih AND iade_tarihi < :bitTarih GROUP BY 1, 3, 4
                """.formatted(tarihAraligi, bitisAraligi), params, (rs, i) -> {
            long id = rs.getLong("kullanici_id");
            Long kullanici = rs.wasNull() ? null : id;
            long hesapId = rs.getLong("hesap_id");
            Long hesap = rs.wasNull() ? null : hesapId;
            return new Hucre(kullanici, rs.getString("kaynak"), rs.getString("yontem"), hesap, rs.getBigDecimal("tutar"));
        });

        // Hesap adları (havalenin hangi bankaya yattığını göstermek için)
        Map<Long, String> hesapAdlari = new LinkedHashMap<>();
        jdbc.query("SELECT id, ad FROM hesap WHERE isletme_id = :isletmeId", params, rs -> {
            hesapAdlari.put(rs.getLong("id"), rs.getString("ad"));
        });

        List<KasaSatiri> satirlar = new ArrayList<>();
        for (Kisi kisi : kisiler(params)) {
            List<Hucre> benim = hucreler.stream().filter(h -> java.util.Objects.equals(h.kullanici(), kisi.id())).toList();
            // "Belirtilmemiş" satırı yalnızca bilgisiz kayıt varsa gösterilir
            if (kisi.id() == null && benim.isEmpty()) {
                continue;
            }
            satirlar.add(kasaSatiri(kisi.id(), kisi.ad(), kisi.rol(), benim.stream()
                    .map(h -> new Object[]{h.kaynak(), h.yontem(), h.tutar(), h.hesap()}).toList(), hesapAdlari));
        }
        // Toplam satırı
        List<Object[]> hepsi = hucreler.stream().map(h -> new Object[]{h.kaynak(), h.yontem(), h.tutar(), h.hesap()}).toList();
        return new KullaniciRaporlari.Kasa(aralik.ilk(), aralik.son(), satirlar, kasaSatiri(null, "Toplam", null, hepsi, hesapAdlari));
    }

    private KasaSatiri kasaSatiri(Long id, String ad, String rol, List<Object[]> hucreler, Map<Long, String> hesapAdlari) {
        BigDecimal rez = BigDecimal.ZERO, ogr = BigDecimal.ZERO, env = BigDecimal.ZERO, abo = BigDecimal.ZERO;
        BigDecimal nakit = BigDecimal.ZERO, kart = BigDecimal.ZERO, havale = BigDecimal.ZERO;
        BigDecimal iade = BigDecimal.ZERO, iadeNakit = BigDecimal.ZERO;
        Map<Long, BigDecimal> havaleHesaplari = new LinkedHashMap<>();
        for (Object[] h : hucreler) {
            String kaynak = (String) h[0];
            String yontem = (String) h[1];
            BigDecimal tutar = (BigDecimal) h[2];
            if ("IADE".equals(kaynak)) {
                iade = iade.add(tutar);
                if ("NAKIT".equals(yontem)) iadeNakit = iadeNakit.add(tutar);
                continue;
            }
            switch (kaynak) {
                case "REZERVASYON" -> rez = rez.add(tutar);
                case "OGRENCI" -> ogr = ogr.add(tutar);
                case "ENVANTER" -> env = env.add(tutar);
                default -> abo = abo.add(tutar);
            }
            if ("NAKIT".equals(yontem)) nakit = nakit.add(tutar);
            else if ("KART".equals(yontem)) kart = kart.add(tutar);
            else if ("HAVALE_EFT".equals(yontem)) {
                havale = havale.add(tutar);
                havaleHesaplari.merge((Long) h[3], tutar, BigDecimal::add);
            }
        }
        BigDecimal tahsilat = rez.add(ogr).add(env).add(abo);
        List<KullaniciRaporlari.HesapTutari> hesaplar = havaleHesaplari.entrySet().stream()
                .map(e -> new KullaniciRaporlari.HesapTutari(e.getKey(), e.getKey() != null ? hesapAdlari.getOrDefault(e.getKey(), "Silinmiş hesap") : "Hesap bilgisi yok", e.getValue()))
                .sorted((a, b) -> b.tutar().compareTo(a.tutar()))
                .toList();
        return new KasaSatiri(id, ad, rol, rez, ogr, env, abo, tahsilat, nakit, kart, havale, iade, iadeNakit,
                tahsilat.subtract(iade), nakit.subtract(iadeNakit), hesaplar);
    }

    // ── 2) İşlemler ──────────────────────────────────────────────────────────────────────────

    public KullaniciRaporlari.Islemler islemler(LocalDate baslangic, LocalDate bitis) {
        RaporAraligi aralik = RaporAraligi.of(baslangic, bitis);
        MapSqlParameterSource params = params(aralik);

        Map<Long, Long> rezervasyon = sayilar("""
                SELECT kullanici_id, COUNT(*) FROM rezervasyon
                WHERE isletme_id = :isletmeId AND abonelik_id IS NULL AND kayit_tarih >= :bas AND kayit_tarih < :bit GROUP BY 1
                """, params);
        Map<Long, Long> iptal = sayilar("""
                SELECT iptal_eden_id, COUNT(*) FROM rezervasyon
                WHERE isletme_id = :isletmeId AND durum = 'IPTAL' AND COALESCE(iptal_neden, '') <> :abonelikIptali
                  AND iptal_tarih >= :bas AND iptal_tarih < :bit GROUP BY 1
                """, params);
        Map<Long, Long> ogrenciOdeme = sayilar("""
                SELECT kullanici_id, COUNT(*) FROM ogrenci_odeme
                WHERE isletme_id = :isletmeId AND odenen_tutar > 0 AND odeme_tarihi >= :basTarih AND odeme_tarihi < :bitTarih GROUP BY 1
                """, params);
        Map<Long, Long> envanter = sayilar("""
                SELECT kullanici_id, COUNT(*) FROM envanter_satis
                WHERE isletme_id = :isletmeId AND kayit_tarihi >= :bas AND kayit_tarihi < :bit GROUP BY 1
                """, params);
        Map<Long, Long> abonelik = sayilar("""
                SELECT kullanici_id, COUNT(*) FROM abonelik
                WHERE isletme_id = :isletmeId AND kayit_tarih >= :bas AND kayit_tarih < :bit GROUP BY 1
                """, params);
        Map<Long, Long> yoklama = sayilar("""
                SELECT kullanici_id, COUNT(DISTINCT tarih) FROM yoklama
                WHERE isletme_id = :isletmeId AND tarih >= :basTarih AND tarih < :bitTarih GROUP BY 1
                """, params);
        Map<Long, Long> iade = sayilar("""
                SELECT kullanici_id, COUNT(*) FROM iade
                WHERE isletme_id = :isletmeId AND iade_tarihi >= :basTarih AND iade_tarihi < :bitTarih GROUP BY 1
                """, params);
        Map<Long, Long> transfer = sayilar("""
                SELECT talep_eden_id, COUNT(*) FROM ogrenci_transfer
                WHERE isletme_id = :isletmeId AND talep_tarihi >= :bas AND talep_tarihi < :bit GROUP BY 1
                """, params);
        Map<Long, Long> giris = sayilar("""
                SELECT kullanici_id, COUNT(*) FROM oturum_log
                WHERE isletme_id = :isletmeId AND olay = 'GIRIS' AND zaman >= :bas AND zaman < :bit GROUP BY 1
                """, params);
        Map<Long, LocalDateTime> sonCikis = sorgu("""
                SELECT kullanici_id, MAX(zaman) FROM oturum_log
                WHERE isletme_id = :isletmeId AND olay IN ('CIKIS', 'OTURUM_SURESI_DOLDU') GROUP BY 1
                """, params, rs -> {
            try {
                Timestamp t = rs.getTimestamp(2);
                return t != null ? t.toLocalDateTime() : null;
            } catch (java.sql.SQLException e) {
                throw new IllegalStateException(e);
            }
        });
        Map<Long, LocalDateTime> sonGiris = sorgu("""
                SELECT kullanici_id, MAX(zaman) FROM oturum_log
                WHERE isletme_id = :isletmeId AND olay = 'GIRIS' GROUP BY 1
                """, params, rs -> {
            try {
                Timestamp t = rs.getTimestamp(2);
                return t != null ? t.toLocalDateTime() : null;
            } catch (java.sql.SQLException e) {
                throw new IllegalStateException(e);
            }
        });

        List<IslemSatiri> satirlar = new ArrayList<>();
        for (Kisi k : kisiler(params)) {
            IslemSatiri s = new IslemSatiri(k.id(), k.ad(), k.rol(),
                    rezervasyon.getOrDefault(k.id(), 0L), iptal.getOrDefault(k.id(), 0L), ogrenciOdeme.getOrDefault(k.id(), 0L),
                    envanter.getOrDefault(k.id(), 0L), abonelik.getOrDefault(k.id(), 0L), yoklama.getOrDefault(k.id(), 0L),
                    iade.getOrDefault(k.id(), 0L), transfer.getOrDefault(k.id(), 0L),
                    giris.getOrDefault(k.id(), 0L), sonGiris.get(k.id()), sonCikis.get(k.id()));
            boolean bos = s.rezervasyon() + s.rezervasyonIptal() + s.ogrenciOdeme() + s.envanterSatis() + s.abonelik()
                    + s.yoklamaGunu() + s.iade() + s.transferTalebi() + s.giris() == 0;
            if (k.id() == null && bos) {
                continue;
            }
            satirlar.add(s);
        }
        return new KullaniciRaporlari.Islemler(aralik.ilk(), aralik.son(), satirlar);
    }

    // ── 3) İadeler ───────────────────────────────────────────────────────────────────────────

    public KullaniciRaporlari.Iadeler iadeler(LocalDate baslangic, LocalDate bitis) {
        RaporAraligi aralik = RaporAraligi.of(baslangic, bitis);
        MapSqlParameterSource params = params(aralik);

        List<IadeKaynakToplami> kaynaklar = jdbc.query("""
                SELECT CASE WHEN rezervasyon_id IS NOT NULL THEN 'REZERVASYON' WHEN ogrenci_odeme_id IS NOT NULL THEN 'OGRENCI'
                            WHEN envanter_satis_id IS NOT NULL THEN 'ENVANTER' ELSE 'ABONELIK' END AS kaynak,
                       COUNT(*) AS adet, SUM(tutar) AS toplam
                FROM iade WHERE isletme_id = :isletmeId AND iade_tarihi >= :basTarih AND iade_tarihi < :bitTarih
                GROUP BY 1 ORDER BY 1
                """, params, (rs, i) -> new IadeKaynakToplami(rs.getString("kaynak"), rs.getLong("adet"), rs.getBigDecimal("toplam")));
        BigDecimal toplam = kaynaklar.stream().map(IadeKaynakToplami::toplam).reduce(BigDecimal.ZERO, BigDecimal::add);

        params.addValue("limit", SATIR_SINIRI + 1);
        List<IadeSatiri> satirlar = new ArrayList<>(jdbc.query("""
                SELECT i.id, i.iade_tarihi, i.tutar, i.iade_sekli, i.neden, k.ad_soyad AS kullanici,
                       CASE WHEN i.rezervasyon_id IS NOT NULL THEN 'REZERVASYON' WHEN i.ogrenci_odeme_id IS NOT NULL THEN 'OGRENCI'
                            WHEN i.envanter_satis_id IS NOT NULL THEN 'ENVANTER' ELSE 'ABONELIK' END AS kaynak,
                       COALESCE(i.rezervasyon_id, i.ogrenci_odeme_id, i.envanter_satis_id, i.abonelik_id) AS kaynak_id,
                       COALESCE(r.ad_soyad, o1.ad_soyad, o2.ad_soyad, a.ad_soyad) AS ilgili
                FROM iade i
                LEFT JOIN kullanici k ON k.id = i.kullanici_id
                LEFT JOIN rezervasyon r ON r.id = i.rezervasyon_id
                LEFT JOIN ogrenci_odeme p ON p.id = i.ogrenci_odeme_id
                LEFT JOIN ogrenci o1 ON o1.id = p.ogrenci_id
                LEFT JOIN envanter_satis s ON s.id = i.envanter_satis_id
                LEFT JOIN ogrenci o2 ON o2.id = s.ogrenci_id
                LEFT JOIN abonelik a ON a.id = i.abonelik_id
                WHERE i.isletme_id = :isletmeId AND i.iade_tarihi >= :basTarih AND i.iade_tarihi < :bitTarih
                ORDER BY i.iade_tarihi DESC, i.id DESC LIMIT :limit
                """, params, (rs, i) -> new IadeSatiri(rs.getLong("id"), rs.getDate("iade_tarihi").toLocalDate(), rs.getString("kaynak"),
                rs.getLong("kaynak_id"), rs.getString("ilgili"), rs.getBigDecimal("tutar"), rs.getString("iade_sekli"),
                rs.getString("neden"), rs.getString("kullanici"))));
        boolean kesildi = satirlar.size() > SATIR_SINIRI;
        if (kesildi) satirlar = satirlar.subList(0, SATIR_SINIRI);
        return new KullaniciRaporlari.Iadeler(aralik.ilk(), aralik.son(), toplam, kaynaklar, satirlar, kesildi);
    }

    // ── 4) İptaller ──────────────────────────────────────────────────────────────────────────

    public KullaniciRaporlari.Iptaller iptaller(LocalDate baslangic, LocalDate bitis) {
        RaporAraligi aralik = RaporAraligi.of(baslangic, bitis);
        MapSqlParameterSource params = params(aralik);
        params.addValue("limit", SATIR_SINIRI + 1);

        // Aboneliğin iptali sonucu iptal edilen haftalık rezervasyonlar ayrıca sayılmaz; iptal tek kayıt olarak aboneliktedir
        String rezervasyonKosulu = """
                r.isletme_id = :isletmeId AND r.durum = 'IPTAL' AND COALESCE(r.iptal_neden, '') <> :abonelikIptali
                AND ((r.iptal_tarih >= :bas AND r.iptal_tarih < :bit)
                     OR (r.iptal_tarih IS NULL AND r.baslangic_tarih >= :bas AND r.baslangic_tarih < :bit))
                """;
        List<IptalSatiri> satirlar = new ArrayList<>(jdbc.query("""
                SELECT 'REZERVASYON' AS tur, r.id, r.ad_soyad, r.baslangic_tarih AS seans, sh.ad AS saha, r.iptal_tarih,
                       k.ad_soyad AS iptal_eden, r.iptal_neden
                FROM rezervasyon r JOIN saha sh ON sh.id = r.saha_id LEFT JOIN kullanici k ON k.id = r.iptal_eden_id
                WHERE %s
                UNION ALL
                SELECT 'ABONELIK', a.id, a.ad_soyad, a.baslangic_tarih::timestamp, sh.ad, a.iptal_tarih, k.ad_soyad, a.iptal_neden
                FROM abonelik a JOIN saha sh ON sh.id = a.saha_id LEFT JOIN kullanici k ON k.id = a.iptal_eden_id
                WHERE a.isletme_id = :isletmeId AND a.durum = 'IPTAL' AND a.iptal_tarih >= :bas AND a.iptal_tarih < :bit
                ORDER BY iptal_tarih DESC NULLS LAST, seans DESC LIMIT :limit
                """.formatted(rezervasyonKosulu), params, (rs, i) -> {
            Timestamp seans = rs.getTimestamp("seans");
            Timestamp iptal = rs.getTimestamp("iptal_tarih");
            return new IptalSatiri(rs.getString("tur"), rs.getLong("id"), rs.getString("ad_soyad"),
                    seans != null ? seans.toLocalDateTime() : null, rs.getString("saha"),
                    iptal != null ? iptal.toLocalDateTime() : null, rs.getString("iptal_eden"), rs.getString("iptal_neden"));
        }));
        boolean kesildi = satirlar.size() > SATIR_SINIRI;
        if (kesildi) satirlar = satirlar.subList(0, SATIR_SINIRI);

        // Kişi bazında sayılar (satır sınırından bağımsız, gerçek toplamlar)
        Map<Long, Long> rezervasyon = sayilar("""
                SELECT r.iptal_eden_id, COUNT(*) FROM rezervasyon r WHERE %s GROUP BY 1
                """.formatted(rezervasyonKosulu), params);
        Map<Long, Long> abonelik = sayilar("""
                SELECT iptal_eden_id, COUNT(*) FROM abonelik
                WHERE isletme_id = :isletmeId AND durum = 'IPTAL' AND iptal_tarih >= :bas AND iptal_tarih < :bit GROUP BY 1
                """, params);
        List<IptalKisi> kisiler = new ArrayList<>();
        long toplam = 0;
        for (Kisi k : kisiler(params)) {
            long r = rezervasyon.getOrDefault(k.id(), 0L);
            long a = abonelik.getOrDefault(k.id(), 0L);
            toplam += r + a;
            if (k.id() == null && r + a == 0) {
                continue;
            }
            kisiler.add(new IptalKisi(k.id(), k.ad(), r, a));
        }
        return new KullaniciRaporlari.Iptaller(aralik.ilk(), aralik.son(), toplam, kisiler, satirlar, kesildi);
    }

    // ── 5) Yoklama ───────────────────────────────────────────────────────────────────────────

    public KullaniciRaporlari.Yoklama yoklama(LocalDate baslangic, LocalDate bitis) {
        RaporAraligi aralik = RaporAraligi.of(baslangic, bitis);
        MapSqlParameterSource params = params(aralik);
        params.addValue("limit", SATIR_SINIRI + 1);

        List<YoklamaGunu> gunler = new ArrayList<>(jdbc.query("""
                SELECT y.tarih, gr.id AS grup_id, COALESCE(gr.ad, 'Grupsuz') AS grup_ad, COALESCE(k.ad_soyad, 'Belirtilmemiş') AS kullanici,
                       COUNT(*) AS ogrenci,
                       COUNT(*) FILTER (WHERE y.durum = 'GELDI') AS geldi,
                       COUNT(*) FILTER (WHERE y.durum = 'GELMEDI') AS gelmedi,
                       COUNT(*) FILTER (WHERE y.durum = 'IZINLI') AS izinli
                FROM yoklama y
                JOIN ogrenci o ON o.id = y.ogrenci_id
                LEFT JOIN grup gr ON gr.id = o.grup_id
                LEFT JOIN kullanici k ON k.id = y.kullanici_id
                WHERE y.isletme_id = :isletmeId AND y.tarih >= :basTarih AND y.tarih < :bitTarih
                GROUP BY y.tarih, gr.id, gr.ad, k.id, k.ad_soyad
                ORDER BY y.tarih DESC, grup_ad LIMIT :limit
                """, params, (rs, i) -> {
            long grupId = rs.getLong("grup_id");
            boolean grupsuz = rs.wasNull();
            return new YoklamaGunu(rs.getDate("tarih").toLocalDate(), grupsuz ? null : grupId, rs.getString("grup_ad"),
                    rs.getString("kullanici"), rs.getLong("ogrenci"), rs.getLong("geldi"), rs.getLong("gelmedi"), rs.getLong("izinli"));
        }));
        boolean kesildi = gunler.size() > SATIR_SINIRI;
        if (kesildi) gunler = gunler.subList(0, SATIR_SINIRI);

        // Yoklamayı alan kullanıcılar (yalnızca dönemde yoklama alanlar listelenir)
        record Ozet(long gun, long grup, LocalDate son) {
        }
        Map<Long, Ozet> ozetler = sorgu("""
                SELECT y.kullanici_id, COUNT(DISTINCT y.tarih) AS gun, COUNT(DISTINCT o.grup_id) AS grup, MAX(y.tarih) AS son
                FROM yoklama y JOIN ogrenci o ON o.id = y.ogrenci_id
                WHERE y.isletme_id = :isletmeId AND y.tarih >= :basTarih AND y.tarih < :bitTarih
                GROUP BY y.kullanici_id
                """, params, rs -> {
            try {
                return new Ozet(rs.getLong("gun"), rs.getLong("grup"), rs.getDate("son").toLocalDate());
            } catch (java.sql.SQLException e) {
                throw new IllegalStateException(e);
            }
        });
        List<YoklamaKullanici> kullanicilar = new ArrayList<>();
        for (Kisi k : kisiler(params)) {
            Ozet o = ozetler.get(k.id());
            if (o != null) {
                kullanicilar.add(new YoklamaKullanici(k.id(), k.ad(), o.gun(), o.grup(), o.son()));
            }
        }
        kullanicilar.sort((a, b) -> Long.compare(b.yoklamaGunu(), a.yoklamaGunu()));
        return new KullaniciRaporlari.Yoklama(aralik.ilk(), aralik.son(), gunler, kullanicilar, kesildi);
    }

    // ── 6) Giriş geçmişi ─────────────────────────────────────────────────────────────────────

    public KullaniciRaporlari.Giris giris(LocalDate baslangic, LocalDate bitis, Long kullaniciId) {
        RaporAraligi aralik = RaporAraligi.of(baslangic, bitis);
        MapSqlParameterSource params = params(aralik);
        params.addValue("limit", SATIR_SINIRI + 1);
        String kullaniciFiltre = "";
        if (kullaniciId != null) {
            kullaniciFiltre = " AND l.kullanici_id = :kullaniciId";
            params.addValue("kullaniciId", kullaniciId);
        }
        List<GirisSatiri> satirlar = new ArrayList<>(jdbc.query("""
                SELECT l.zaman, l.kullanici_id, l.kullanici_adi, k.ad_soyad, l.olay, l.ip, l.tarayici
                FROM oturum_log l LEFT JOIN kullanici k ON k.id = l.kullanici_id
                WHERE l.isletme_id = :isletmeId AND l.zaman >= :bas AND l.zaman < :bit%s
                ORDER BY l.zaman DESC LIMIT :limit
                """.formatted(kullaniciFiltre), params, (rs, i) -> {
            long id = rs.getLong("kullanici_id");
            return new GirisSatiri(rs.getTimestamp("zaman").toLocalDateTime(), rs.wasNull() ? null : id, rs.getString("kullanici_adi"),
                    rs.getString("ad_soyad"), rs.getString("olay"), rs.getString("ip"), rs.getString("tarayici"));
        }));
        boolean kesildi = satirlar.size() > SATIR_SINIRI;
        if (kesildi) satirlar = satirlar.subList(0, SATIR_SINIRI);
        return new KullaniciRaporlari.Giris(aralik.ilk(), aralik.son(), satirlar, kesildi);
    }
}
