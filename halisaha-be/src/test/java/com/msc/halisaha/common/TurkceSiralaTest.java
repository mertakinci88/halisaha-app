package com.msc.halisaha.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TurkceSiralaTest {

    @Test
    void turkceHarflerAlfabeSirasindadir() {
        List<String> sirali = List.of("Şeker", "Su", "Ömer", "İpek", "Irmak", "Çilek", "Cips", "Zeynep", "Ahmet", "Üzüm", "Ğ Takımı", "Gül")
                .stream().sorted(TurkceSirala.alfabetik(s -> s)).toList();
        assertEquals(List.of("Ahmet", "Cips", "Çilek", "Gül", "Ğ Takımı", "Irmak", "İpek", "Ömer", "Su", "Şeker", "Üzüm", "Zeynep"), sirali);
    }

    @Test
    void buyukKucukHarfSiralamayiBozmaz() {
        List<String> sirali = List.of("kola", "Cips", "Ayran", "su")
                .stream().sorted(TurkceSirala.alfabetik(s -> s)).toList();
        assertEquals(List.of("Ayran", "Cips", "kola", "su"), sirali);
    }

    @Test
    void bosDegerlerSonaGider() {
        List<String> sirali = java.util.Arrays.asList("b", null, "a")
                .stream().sorted(TurkceSirala.<String>alfabetik(s -> s)).toList();
        assertEquals(java.util.Arrays.asList("a", "b", null), sirali);
    }
}
