package com.msc.halisaha.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OgrenciOdemeDurumuTest {

    private static OgrenciOdemeDurumu durum(boolean odeme, int yil, int ay, String bugun) {
        return OgrenciOdemeDurumu.hesapla(odeme, yil, ay, LocalDate.parse(bugun));
    }

    @Test
    void odemeVarsaHerZamanOdendi() {
        assertEquals(OgrenciOdemeDurumu.ODENDI, durum(true, 2026, 10, "2026-10-30"));
        assertEquals(OgrenciOdemeDurumu.ODENDI, durum(true, 2027, 1, "2026-10-01"));
    }

    @Test
    void ayinOnBesineKadarOdemeBekliyor() {
        assertEquals(OgrenciOdemeDurumu.ODEME_BEKLIYOR, durum(false, 2026, 10, "2026-10-01"));
        assertEquals(OgrenciOdemeDurumu.ODEME_BEKLIYOR, durum(false, 2026, 10, "2026-10-15"));   // 15'i dahil
    }

    @Test
    void onAltidanSonraOdenmedi() {
        assertEquals(OgrenciOdemeDurumu.ODENMEDI, durum(false, 2026, 10, "2026-10-16"));
        assertEquals(OgrenciOdemeDurumu.ODENMEDI, durum(false, 2026, 10, "2026-10-31"));
    }

    @Test
    void gecmisAyOdenmediGelecekAyBekliyor() {
        assertEquals(OgrenciOdemeDurumu.ODENMEDI, durum(false, 2026, 9, "2026-10-02"));
        assertEquals(OgrenciOdemeDurumu.ODENMEDI, durum(false, 2025, 12, "2026-01-01"));
        assertEquals(OgrenciOdemeDurumu.ODEME_BEKLIYOR, durum(false, 2026, 11, "2026-10-30"));
        assertEquals(OgrenciOdemeDurumu.ODEME_BEKLIYOR, durum(false, 2027, 1, "2026-10-30"));
    }
}
