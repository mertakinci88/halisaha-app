package com.msc.halisaha.service;

import com.msc.halisaha.common.AppZaman;
import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.common.security.IsletmeContext;
import com.msc.halisaha.dto.TransferKararRequest;
import com.msc.halisaha.dto.TransferResponse;
import com.msc.halisaha.dto.TransferTalepRequest;
import com.msc.halisaha.entity.Grup;
import com.msc.halisaha.entity.Ogrenci;
import com.msc.halisaha.entity.OgrenciTransfer;
import com.msc.halisaha.entity.TransferDurum;
import com.msc.halisaha.repository.GrupRepository;
import com.msc.halisaha.repository.OgrenciRepository;
import com.msc.halisaha.repository.OgrenciTransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Öğrenci grup transferi: talep açılır (BEKLIYOR), admin onaylar (öğrenci hedef gruba geçer) ya da reddeder.
 * Bekleyen talep onaydan önce iptal edilebilir. Öğrencinin tüm talepleri transfer geçmişi olarak listelenir.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OgrenciTransferService {

    private final OgrenciTransferRepository transferRepository;
    private final OgrenciRepository ogrenciRepository;
    private final GrupRepository grupRepository;
    private final IsletmeContext isletmeContext;
    private final MevcutKullanici mevcutKullanici;
    private final GrupKapsami grupKapsami;

    /** Admin ekranı: durum boşsa tüm talepler. */
    public List<TransferResponse> listele(TransferDurum durum) {
        return transferRepository.listele(isletmeContext.id(), durum).stream()
                .filter(this::gorunur)
                .map(TransferResponse::from).toList();
    }

    public long bekleyenSayisi() {
        if (grupKapsami.kisitli()) {
            return transferRepository.listele(isletmeContext.id(), TransferDurum.BEKLIYOR).stream().filter(this::gorunur).count();
        }
        return transferRepository.countByIsletmeIdAndDurum(isletmeContext.id(), TransferDurum.BEKLIYOR);
    }

    public List<TransferResponse> ogrenciGecmisi(Long ogrenciId) {
        Long isletmeId = isletmeContext.id();
        ogrenciGetir(ogrenciId, isletmeId);
        return transferRepository.findByOgrenciIdAndIsletmeIdOrderByTalepTarihiDescIdDesc(ogrenciId, isletmeId).stream()
                .map(TransferResponse::from).toList();
    }

    @Transactional
    public TransferResponse talepEt(Long ogrenciId, TransferTalepRequest request) {
        Long isletmeId = isletmeContext.id();
        Ogrenci ogrenci = ogrenciGetir(ogrenciId, isletmeId);
        Grup hedef = grupRepository.findByIdAndIsletmeId(request.hedefGrupId(), isletmeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Grup bulunamadı: " + request.hedefGrupId()));
        Grup kaynak = ogrenci.getGrup();
        if (kaynak != null && kaynak.getId().equals(hedef.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Öğrenci zaten \"" + hedef.getAd() + "\" grubunda");
        }
        if (transferRepository.existsByOgrenciIdAndDurum(ogrenciId, TransferDurum.BEKLIYOR)) {
            throw new ApiException(HttpStatus.CONFLICT, "Bu öğrencinin onay bekleyen bir transfer talebi zaten var");
        }
        OgrenciTransfer transfer = OgrenciTransfer.builder()
                .isletme(isletmeContext.getir())
                .ogrenci(ogrenci)
                .kaynakGrup(kaynak)
                .kaynakGrupAd(kaynak != null ? kaynak.getAd() : null)
                .hedefGrup(hedef)
                .hedefGrupAd(hedef.getAd())
                .aciklama(bosIseNull(request.aciklama()))
                .talepEden(mevcutKullanici.getir())
                .build();
        return TransferResponse.from(transferRepository.save(transfer));
    }

    /** Bekleyen talebin hedef grubu ve açıklaması değiştirilebilir; onay / red / iptal sonrası değiştirilemez. */
    @Transactional
    public TransferResponse guncelle(Long transferId, TransferTalepRequest request) {
        OgrenciTransfer transfer = bekleyenGetir(transferId);
        Grup hedef = grupRepository.findByIdAndIsletmeId(request.hedefGrupId(), isletmeContext.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Grup bulunamadı: " + request.hedefGrupId()));
        Grup kaynak = transfer.getOgrenci().getGrup();
        if (kaynak != null && kaynak.getId().equals(hedef.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Öğrenci zaten \"" + hedef.getAd() + "\" grubunda");
        }
        transfer.setHedefGrup(hedef);
        transfer.setHedefGrupAd(hedef.getAd());
        transfer.setAciklama(bosIseNull(request.aciklama()));
        return TransferResponse.from(transfer);
    }

    /** Öğrenci hedef gruba geçer; nereden geçtiği, talep sonrası değişmiş olsa bile onay anındaki gerçek grup olarak yazılır. */
    @Transactional
    public TransferResponse onayla(Long transferId) {
        OgrenciTransfer transfer = bekleyenGetir(transferId);
        Grup hedef = transfer.getHedefGrup();
        if (hedef == null) {
            throw new ApiException(HttpStatus.CONFLICT, "Hedef grup silinmiş; talebi reddedin");
        }
        Ogrenci ogrenci = transfer.getOgrenci();
        Grup simdiki = ogrenci.getGrup();
        if (simdiki != null && simdiki.getId().equals(hedef.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "Öğrenci zaten \"" + hedef.getAd() + "\" grubunda; talebi reddedin");
        }
        transfer.setKaynakGrup(simdiki);
        transfer.setKaynakGrupAd(simdiki != null ? simdiki.getAd() : null);
        transfer.setHedefGrupAd(hedef.getAd());
        ogrenci.setGrup(hedef);
        karar(transfer, TransferDurum.ONAYLANDI, null);
        return TransferResponse.from(transfer);
    }

    @Transactional
    public TransferResponse reddet(Long transferId, TransferKararRequest request) {
        OgrenciTransfer transfer = bekleyenGetir(transferId);
        karar(transfer, TransferDurum.REDDEDILDI, request != null ? request.not() : null);
        return TransferResponse.from(transfer);
    }

    @Transactional
    public TransferResponse iptalEt(Long transferId, TransferKararRequest request) {
        OgrenciTransfer transfer = bekleyenGetir(transferId);
        karar(transfer, TransferDurum.IPTAL, request != null ? request.not() : null);
        return TransferResponse.from(transfer);
    }

    private void karar(OgrenciTransfer transfer, TransferDurum durum, String not) {
        transfer.setDurum(durum);
        transfer.setKararVeren(mevcutKullanici.getir());
        transfer.setKararTarihi(AppZaman.simdi());
        transfer.setKararNotu(bosIseNull(not));
    }

    private OgrenciTransfer bekleyenGetir(Long transferId) {
        OgrenciTransfer transfer = transferRepository.findByIdAndIsletmeId(transferId, isletmeContext.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Transfer talebi bulunamadı: " + transferId));
        if (!gorunur(transfer)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Transfer talebi bulunamadı: " + transferId);
        }
        if (transfer.getDurum() != TransferDurum.BEKLIYOR) {
            throw new ApiException(HttpStatus.CONFLICT, "Bu talep zaten sonuçlandırılmış");
        }
        return transfer;
    }

    /** Grup kısıtlı personel yalnızca öğrencisi şu an izinli gruplarda olan talepleri görür. */
    private boolean gorunur(OgrenciTransfer transfer) {
        Grup grup = transfer.getOgrenci().getGrup();
        return grupKapsami.grupIzinli(grup != null ? grup.getId() : null);
    }

    private Ogrenci ogrenciGetir(Long ogrenciId, Long isletmeId) {
        Ogrenci ogrenci = ogrenciRepository.findByIdAndIsletmeId(ogrenciId, isletmeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Öğrenci bulunamadı: " + ogrenciId));
        grupKapsami.kontrol(ogrenci);
        return ogrenci;
    }

    private static String bosIseNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
