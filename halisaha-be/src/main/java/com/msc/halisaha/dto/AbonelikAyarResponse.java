package com.msc.halisaha.dto;

import java.math.BigDecimal;

/** Standart depozito tutarı (alan adı eski kalmıştır); tanımlanmamışsa {@code standartUcret} null gelir. */
public record AbonelikAyarResponse(BigDecimal standartUcret) {
}
