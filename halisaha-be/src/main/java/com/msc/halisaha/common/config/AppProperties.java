package com.msc.halisaha.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors) {

    public record Jwt(String secret, long accessTokenExpirationMs, long refreshTokenExpirationMs) {
    }

    public record Cors(List<String> allowedOrigins) {
    }
}
