package com.msc.halisaha.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Spring Security'nin varsayılan giriş noktası (Http403ForbiddenEntryPoint) kimliksiz/süresi
 * dolmuş istekler için 403 döner; JWT tabanlı SPA akışında frontend'in refresh/login
 * yönlendirmesini tetikleyebilmesi için burada gerçek 401 dönülüyor. Gövde, GlobalExceptionHandler'ın
 * ErrorResponse(status, message, timestamp) şeklini elle üretir; bu noktada Spring MVC devrede
 * olmadığından @RestControllerAdvice devreye girmiyor.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String message = "Oturum süresi doldu veya geçersiz, lütfen tekrar giriş yapın";
        String body = "{\"status\":401,\"message\":\"" + escape(message) + "\",\"timestamp\":\"" + Instant.now() + "\"}";
        response.getWriter().write(body);
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
