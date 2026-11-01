package hr.span.akademija.vulnshop.lab11;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lab11 – A07 Authentication Failures: brute force i enumeracija korisnika
 * (CWE-307, CWE-204).
 *
 * RANJIVO: razliciti odgovori za nepostojeceg korisnika (404 "Korisnik ne postoji")
 * i pogresnu lozinku (401 "Pogresna lozinka") omogucuju enumeraciju korisnickih
 * imena. Nema ogranicenja broja pokusaja, pa je moguc neometan brute force.
 */
@RestController
@RequestMapping("/api/lab11")
public class Lab11Controller {

    private final JdbcTemplate jdbc;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public Lab11Controller(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record LoginRequest(String username, String password) {
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest body) {
        String username = body.username() == null ? "" : body.username();
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT password_hash FROM lab11_users WHERE username = ?", username);
        // RANJIVO: posebna poruka/status za nepostojeceg korisnika omogucuje enumeraciju.
        if (rows.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Korisnik ne postoji."));
        }
        String hash = (String) rows.get(0).get("password_hash");
        // RANJIVO: nema brojaca pokusaja ni zakljucavanja.
        if (!encoder.matches(body.password() == null ? "" : body.password(), hash)) {
            return ResponseEntity.status(401).body(Map.of("error", "Pogresna lozinka."));
        }
        return ResponseEntity.ok(Map.of("status", "prijavljen", "username", username));
    }
}
