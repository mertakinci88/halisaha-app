package com.msc.halisaha.dto;

import java.math.BigDecimal;

/** Standart ücret tanımlanmamışsa {@code standartUcret} null gelir. */
public record AbonelikAyarResponse(BigDecimal standartUcret) {
}
