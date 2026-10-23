package com.msc.halisaha.common;

import com.msc.halisaha.common.exception.ApiException;
import com.msc.halisaha.common.security.GirisDenemeSinirlayici;
import com.msc.halisaha.common.security.IstemciBilgisi;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GirisDenemeSinirlayiciTest {

    private final IstemciBilgisi bilgi = new IstemciBilgisi("1.2.3.4", "T");

    @Test
    void besBasarisizDenemedenSonraAdresKullaniciEngellenir() {
        GirisDenemeSinirlayici s = new GirisDenemeSinirlayici();
        for (int i = 0; i < 5; i++) {
            s.kontrolEt(bilgi, "admin");
            s.basarisiz(bilgi, "admin");
        }
        ApiException e = assertThrows(ApiException.class, () -> s.kontrolEt(bilgi, "admin"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getStatus());
        // Aynı adresten başka kullanıcı ve başka adresten aynı kullanıcı etkilenmez
        assertDoesNotThrow(() -> s.kontrolEt(bilgi, "baska"));
        assertDoesNotThrow(() -> s.kontrolEt(new IstemciBilgisi("9.9.9.9", "T"), "admin"));
    }

    @Test
    void basariliGirisSayaciSifirlar() {
        GirisDenemeSinirlayici s = new GirisDenemeSinirlayici();
        for (int i = 0; i < 4; i++) s.basarisiz(bilgi, "admin");
        s.basarili(bilgi, "admin");
        s.basarisiz(bilgi, "admin");
        assertDoesNotThrow(() -> s.kontrolEt(bilgi, "admin"));
    }

    @Test
    void ayniAdresinFarkliKullaniciDenemeleriToplamdaEngellenir() {
        GirisDenemeSinirlayici s = new GirisDenemeSinirlayici();
        for (int i = 0; i < 15; i++) s.basarisiz(bilgi, "u" + i);
        assertThrows(ApiException.class, () -> s.kontrolEt(bilgi, "yeni"));
    }
}
