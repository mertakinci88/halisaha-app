package com.msc.halisaha.common.security;

import jakarta.servlet.http.HttpServletRequest;

/** İsteği yapan istemcinin adresi ve tarayıcısı (giriş/çıkış kayıtları için). */
public record IstemciBilgisi(String ip, String tarayici) {

    public static final IstemciBilgisi BILINMIYOR = new IstemciBilgisi(null, null);

    /** Ters vekil (proxy) arkasında X-Forwarded-For'un ilk adresi, yoksa doğrudan bağlantı adresi. */
    public static IstemciBilgisi from(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            ip = ip.split(",")[0].trim();
        } else {
            ip = request.getRemoteAddr();
        }
        return new IstemciBilgisi(kisalt(ip, 64), kisalt(request.getHeader("User-Agent"), 255));
    }

    private static String kisalt(String metin, int sinir) {
        return metin == null ? null : metin.length() > sinir ? metin.substring(0, sinir) : metin;
    }
}
