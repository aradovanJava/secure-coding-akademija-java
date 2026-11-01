package hr.span.akademija.vulnshop.common;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Jednostavna pohrana tokena u memoriji za zajednicku autentikaciju.
 * Ovo NIJE dio vjezbi i implementirano je ispravno (nasumicni token od 32 bajta).
 *
 * U stvarnoj aplikaciji koristili bismo stateless tokene (JWT) ili sesijsku
 * pohranu u bazi/Redisu; za potrebe akademije dovoljna je mapa u memoriji.
 */
@Component
public class TokenStore {

    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<String, CurrentUser> tokens = new ConcurrentHashMap<>();

    /** Izdaje novi nasumicni token (32 bajta, base64url bez paddinga) i veze ga uz korisnika. */
    public String issue(CurrentUser user) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.put(token, user);
        return token;
    }

    /** Vraca korisnika za dani token ili {@code null} ako token ne postoji. */
    public CurrentUser resolve(String token) {
        if (token == null) {
            return null;
        }
        return tokens.get(token);
    }
}
