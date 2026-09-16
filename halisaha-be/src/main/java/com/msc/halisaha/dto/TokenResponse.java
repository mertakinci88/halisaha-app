package com.msc.halisaha.dto;

public record TokenResponse(String accessToken, String refreshToken, String tokenType) {
}
