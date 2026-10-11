package com.msc.halisaha.service;

import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.entity.Envanter;
import com.msc.halisaha.entity.EnvanterSatis;
import com.msc.halisaha.entity.EnvanterStokHareket;
import com.msc.halisaha.entity.EnvanterStokHareketTip;
import com.msc.halisaha.repository.EnvanterRepository;
import com.msc.halisaha.repository.EnvanterStokHareketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Envanter stoğundaki her değişiklik buradan geçer; stok güncellemesi ve hareket kaydı aynı transaction'dadır. */
@Service
@RequiredArgsConstructor
public class EnvanterStokService {

    private final EnvanterRepository envanterRepository;
    private final EnvanterStokHareketRepository hareketRepository;
    private final MevcutKullanici mevcutKullanici;

    public static String ad(Envanter e) {
        return e.getTur().getAd() + " · " + e.getBeden();
    }

    /** Teslim edilen envanteri stoktan düşer; stok yetersizse 409 fırlatır ve hiçbir şey değişmez. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void teslimEt(Envanter envanter, EnvanterSatis satis, String neden) {
        if (envanterRepository.stokDus(envanter.getId()) == 0) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Yetersiz stok: " + ad(envanter) + " (kalan " + envanterRepository.mevcutAdet(envanter.getId()) + ")");
        }
        kaydet(envanter.getId(), satis, EnvanterStokHareketTip.TESLIM, -1, neden);
    }

    /** Teslim geri alındığında ya da satış silindiğinde envanteri stoğa geri ekler. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void iade(Envanter envanter, EnvanterSatis satis, String neden) {
        envanterRepository.stokEkle(envanter.getId());
        kaydet(envanter.getId(), satis, EnvanterStokHareketTip.IADE, 1, neden);
    }

    /** Envanter ekranından yapılan elle stok değişikliği (ilk giriş dahil). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void duzelt(Envanter envanter, int eskiAdet, int yeniAdet, String neden) {
        if (eskiAdet == yeniAdet) {
            return;
        }
        hareketRepository.save(EnvanterStokHareket.builder()
                .envanterId(envanter.getId())
                .tip(EnvanterStokHareketTip.DUZELTME)
                .degisim(yeniAdet - eskiAdet)
                .kalanAdet(yeniAdet)
                .aciklama(neden)
                .kullanici(mevcutKullanici.getir())
                .build());
    }

    private void kaydet(Long envanterId, EnvanterSatis satis, EnvanterStokHareketTip tip, int degisim, String neden) {
        String aciklama = satis != null && satis.getOgrenci() != null
                ? "Satış #" + satis.getId() + " · " + satis.getOgrenci().getAdSoyad() + " · " + neden
                : neden;
        hareketRepository.save(EnvanterStokHareket.builder()
                .envanterId(envanterId)
                .satisId(satis != null ? satis.getId() : null)
                .tip(tip)
                .degisim(degisim)
                .kalanAdet(envanterRepository.mevcutAdet(envanterId))
                .aciklama(aciklama)
                .kullanici(mevcutKullanici.getir())
                .build());
    }
}
