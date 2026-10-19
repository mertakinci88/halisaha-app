package com.msc.halisaha.dto;

/**
 * Aboneliği antrenmana (ücretsiz) çevirir. Abonelikte alınmış depozito ödemesi varsa {@code odemeleriSil} true gönderilmelidir;
 * bu durumda ödeme kayıtları silinir (kasadan düşer).
 */
public record AntrenmanaCevirRequest(Boolean odemeleriSil) {
}
