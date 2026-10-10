package com.msc.halisaha.service;

import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.entity.Rezervasyon;
import com.msc.halisaha.entity.StokHareket;
import com.msc.halisaha.entity.StokHareketTip;
import com.msc.halisaha.entity.Urun;
import com.msc.halisaha.repository.StokHareketRepository;
import com.msc.halisaha.repository.UrunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Ürün stoğundaki her değişiklik buradan geçer; stok güncellemesi ve hareket kaydı aynı transaction'dadır. */
@Service
@RequiredArgsConstructor
public class StokService {

    private final UrunRepository urunRepository;
    private final StokHareketRepository stokHareketRepository;
    private final MevcutKullanici mevcutKullanici;

    /** Rezervasyona eklenen ürünü stoktan düşer; stok yetersizse 409 fırlatır ve hiçbir şey değişmez. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void dus(Urun urun, int miktar, Rezervasyon rezervasyon) {
        if (urunRepository.stokDus(urun.getId(), miktar) == 0) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Yetersiz stok: " + urun.getAd() + " (kalan " + urunRepository.mevcutAdet(urun.getId()) + ")");
        }
        kaydet(urun.getId(), rezervasyon, StokHareketTip.SATIS, -miktar, urunRepository.mevcutAdet(urun.getId()), "satış");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void iade(Long urunId, int miktar, Rezervasyon rezervasyon, String neden) {
        urunRepository.stokEkle(urunId, miktar);
        kaydet(urunId, rezervasyon, StokHareketTip.IADE, miktar, urunRepository.mevcutAdet(urunId), neden);
    }

    /** Ürün ekranından yapılan elle stok değişikliği (sayım/düzeltme). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void duzelt(Urun urun, int eskiAdet, int yeniAdet) {
        if (eskiAdet == yeniAdet) {
            return;
        }
        kaydet(urun.getId(), null, StokHareketTip.DUZELTME, yeniAdet - eskiAdet, yeniAdet, "elle düzeltme");
    }

    private void kaydet(Long urunId, Rezervasyon rezervasyon, StokHareketTip tip, int degisim, int kalanAdet, String neden) {
        String aciklama = rezervasyon != null
                ? "Rezervasyon #" + rezervasyon.getId() + " · " + rezervasyon.getAdSoyad() + " · " + neden
                : neden;
        stokHareketRepository.save(StokHareket.builder()
                .urunId(urunId)
                .rezervasyonId(rezervasyon != null ? rezervasyon.getId() : null)
                .tip(tip)
                .degisim(degisim)
                .kalanAdet(kalanAdet)
                .aciklama(aciklama)
                .kullanici(mevcutKullanici.getir())
                .build());
    }
}
