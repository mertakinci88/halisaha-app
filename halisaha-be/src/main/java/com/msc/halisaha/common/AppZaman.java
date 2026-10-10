package com.msc.halisaha.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** İşletme yerel saatinde çalışır; sunucu (container) saat dilimi UTC olsa da tahsilat saatleri doğru yazılır. */
public final class AppZaman {

    public static final ZoneId ISLETME = ZoneId.of("Europe/Istanbul");

    private AppZaman() {
    }

    public static LocalDateTime simdi() {
        return LocalDateTime.now(ISLETME);
    }

    public static LocalDate bugun() {
        return LocalDate.now(ISLETME);
    }
}
