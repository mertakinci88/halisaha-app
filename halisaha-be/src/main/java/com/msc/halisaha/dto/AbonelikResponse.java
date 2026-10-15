package com.msc.halisaha.dto;

import com.msc.halisaha.entity.Abonelik;
import com.msc.halisaha.entity.AbonelikDurum;
import com.msc.halisaha.entity.AbonelikOdeme;
import com.msc.halisaha.entity.OdemeDurumu;
import com.msc.halisaha.entity.OdemeYontemi;
import com.msc.halisaha.entity.RezervasyonDurum;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AbonelikResponse(
        Long id,
        Long sahaId,
        String sahaAd,
        String adSoyad,
        String telNo,
        int haftaGunu,
        /** Gün başından itibaren dakika (12:00 = 720). */
        int baslangicDakika,
        int sureDakika,
        LocalDate baslangicTarih,
        LocalDate bitisTarih,
        AbonelikDurum durum,
        /** Durum aktif olduğu halde bitiş tarihi geçmişse true. */
        boolean suresiDoldu,
        /** Anlaşılan depozito ve maç başına (haftalık) ücret. */
        BigDecimal depozito,
        BigDecimal haftalikUcret,
        /** Alınan depozito ödemeleri toplamı ve depozitonun henüz alınmamış kısmı. */
        BigDecimal odenenTutar,
        BigDecimal kalanTutar,
        OdemeDurumu odemeDurumu,
        /** Depozitodan iade edilen toplam ve işletmede kalan depozito (alınan - iade). */
        BigDecimal iadeTutar,
        BigDecimal kalanDepozito,
        String not,
        LocalDateTime iptalTarih,
        /** İptal eden kullanıcının adı ve neden (eski kayıtlarda boş olabilir). */
        String iptalEdenAd,
        String iptalNeden,
        LocalDateTime kayitTarih,
        String kaydedenAd,
        List<OdemeResponse> odemeler,
        /** Yalnızca detayda dolu: aboneliğin haftalık rezervasyonları (tarih sırasıyla). */
        List<HaftaResponse> haftalar
) {

    public record OdemeResponse(Long id, BigDecimal tutar, OdemeYontemi odemeSekli, Long hesapId, String hesapAd,
                                LocalDate odemeTarihi, String kaydedenAd) {

        static OdemeResponse from(AbonelikOdeme o) {
            return new OdemeResponse(o.getId(), o.getTutar(), o.getOdemeSekli(),
                    o.getHesap() != null ? o.getHesap().getId() : null,
                    o.getHesap() != null ? o.getHesap().getAd() : null,
                    o.getOdemeTarihi(),
                    o.getKullanici() != null ? o.getKullanici().getAdSoyad() : null);
        }
    }

    /** Haftalık rezervasyon ve ödeme durumu: ödenecek tutar (ürünler dahil), alınan ve kalan. */
    public record HaftaResponse(Long rezervasyonId, LocalDateTime baslangicTarih, LocalDateTime bitisTarih, RezervasyonDurum durum,
                                BigDecimal odenecekTutar, BigDecimal odenenTutar, BigDecimal kalanTutar, OdemeDurumu odemeDurumu) {
    }

    public static AbonelikResponse from(Abonelik a, LocalDate bugun, List<HaftaResponse> haftalar, BigDecimal iade) {
        BigDecimal odenen = a.getOdemeler().stream().map(AbonelikOdeme::getTutar).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AbonelikResponse(
                a.getId(), a.getSaha().getId(), a.getSaha().getAd(), a.getAdSoyad(), a.getTelNo(),
                a.getHaftaGunu(), a.getBaslangicDakika(), a.getSureDakika(), a.getBaslangicTarih(), a.getBitisTarih(),
                a.getDurum(),
                a.getDurum() == AbonelikDurum.AKTIF && a.getBitisTarih().isBefore(bugun),
                a.getDepozito(), a.getHaftalikUcret(), odenen, a.getDepozito().subtract(odenen).max(BigDecimal.ZERO),
                OdemeDurumu.hesapla(odenen, a.getDepozito()),
                iade, odenen.subtract(iade).max(BigDecimal.ZERO),
                a.getNot(), a.getIptalTarih(),
                a.getIptalEden() != null ? a.getIptalEden().getAdSoyad() : null, a.getIptalNeden(), a.getKayitTarih(),
                a.getKullanici() != null ? a.getKullanici().getAdSoyad() : null,
                a.getOdemeler().stream().map(OdemeResponse::from).toList(),
                haftalar);
    }
}
