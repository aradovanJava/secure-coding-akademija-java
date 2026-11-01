package hr.span.akademija.vulnshop.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter koji iz zaglavlja {@code Authorization: Bearer <token>} razrjesava
 * trenutnog korisnika i sprema ga u atribut zahtjeva pod kljucem
 * {@link #CURRENT_USER_ATTR}. Ne radi nikakvo ogranicavanje pristupa samo po
 * sebi; pojedini kontroleri (preko {@link CurrentUserArgumentResolver}) odlucuju
 * trazi li se prijava.
 *
 * Ovo je zajednicka, ISPRAVNA autentikacija i nije predmet vjezbi.
 */
@Component
@Order(1)
public class AuthFilter extends OncePerRequestFilter {

    public static final String CURRENT_USER_ATTR = "hr.span.akademija.currentUser";

    private final TokenStore tokenStore;

    public AuthFilter(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring("Bearer ".length()).trim();
            CurrentUser user = tokenStore.resolve(token);
            if (user != null) {
                request.setAttribute(CURRENT_USER_ATTR, user);
            }
        }
        filterChain.doFilter(request, response);
    }
}
