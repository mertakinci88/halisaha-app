package com.msc.halisaha.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, SuperAdmin superAdmin) {

    public record Jwt(String secret, long accessTokenExpirationMs, long refreshTokenExpirationMs) {
    }

    /** İlk açılışta süper admin yoksa bu bilgilerle oluşturulur. Üretimde şifre ortam değişkeniyle verilmelidir. */
    public record SuperAdmin(String username, String password, String adSoyad) {
    }

    public record Cors(List<String> allowedOrigins) {
    }
}
