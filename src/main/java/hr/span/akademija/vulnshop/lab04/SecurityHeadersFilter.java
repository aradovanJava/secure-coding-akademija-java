package hr.span.akademija.vulnshop.lab04;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lab04 – POPRAVAK: dodaje sigurnosna HTTP zaglavlja na sve odgovore.
 *
 * Postoji samo na grani rjesenja. Zaglavlja:
 *  - X-Content-Type-Options: nosniff (CWE-693)
 *  - X-Frame-Options: DENY (zastita od clickjackinga)
 *  - Content-Security-Policy: default-src 'self'
 *  - Referrer-Policy: no-referrer
 */
@Component
@Order(2)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        // Zajednicki (baseline) CSP. Lab08 stranice imaju vlastiti, specificniji CSP
        // (sa script-src/object-src), pa ovdje ne postavljamo CSP za /lab08/** kako ne
        // bismo dobili dva CSP zaglavlja.
        if (!request.getRequestURI().startsWith("/lab08")) {
            response.setHeader("Content-Security-Policy", "default-src 'self'");
        }
        filterChain.doFilter(request, response);
    }
}
