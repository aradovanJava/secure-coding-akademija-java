package hr.span.akademija.vulnshop.lab11;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lab11 – A07 Authentication Failures: brute force i enumeracija (POPRAVLJENO).
 *
 * POPRAVAK:
 *  - jedinstven odgovor 401 "Neispravno korisnicko ime ili lozinka" za oba slucaja
 *    (nepostojeci korisnik i kriva lozinka) -> nema enumeracije,
 *  - provjera lozinke se radi i za nepostojeceg korisnika (dummy BCrypt hash) radi
 *    ujednacenog vremena odgovora,
 *  - zakljucavanje nakon 5 neuspjelih pokusaja na 15 minuta -> 429.
 */
@RestController
@RequestMapping("/api/lab11")
public class Lab11Controller {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final String GENERIC_ERROR = "Neispravno korisnicko ime ili lozinka.";
    // Dummy hash lozinke koja se nikad ne koristi (BCrypt "dummy") – za ujednaceno vrijeme.
    private static final String DUMMY_HASH = new BCryptPasswordEncoder().encode("dummy-password-placeholder");

    private final JdbcTemplate jdbc;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // Jednostavan brojac pokusaja po korisniku u memoriji (u produkciji: Redis + rate limit po IP-u).
    private final ConcurrentHashMap<String, Attempts> attempts = new ConcurrentHashMap<>();

    public Lab11Controller(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final class Attempts {
        int count;
        Instant lockedUntil;
    }

    public record LoginRequest(String username, String password) {
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest body) {
        String username = body.username() == null ? "" : body.username();
        String password = body.password() == null ? "" : body.password();

        // POPRAVAK: provjera zakljucavanja prije svega.
        Attempts state = attempts.computeIfAbsent(username, k -> new Attempts());
        synchronized (state) {
            if (state.lockedUntil != null && Instant.now().isBefore(state.lockedUntil)) {
                return ResponseEntity.status(429).body(Map.of(
                        "error", "Previse neuspjelih pokusaja. Pokusajte kasnije."));
            }

            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT password_hash FROM lab11_users WHERE username = ?", username);

            boolean ok;
            if (rows.isEmpty()) {
                // POPRAVAK: izracunaj hash i za nepostojeceg korisnika (ujednaceno vrijeme), rezultat uvijek false.
                encoder.matches(password, DUMMY_HASH);
                ok = false;
            } else {
                ok = encoder.matches(password, (String) rows.get(0).get("password_hash"));
            }

            if (!ok) {
                state.count++;
                if (state.count >= MAX_ATTEMPTS) {
                    state.lockedUntil = Instant.now().plus(LOCK_DURATION);
                }
                // POPRAVAK: jedinstvena poruka i status za oba slucaja.
                return ResponseEntity.status(401).body(Map.of("error", GENERIC_ERROR));
            }

            // Uspjeh: reset brojaca.
            state.count = 0;
            state.lockedUntil = null;
            return ResponseEntity.ok(Map.of("status", "prijavljen", "username", username));
        }
    }
}
