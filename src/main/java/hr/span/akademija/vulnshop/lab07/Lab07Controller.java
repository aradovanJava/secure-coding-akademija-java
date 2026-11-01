package hr.span.akademija.vulnshop.lab07;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lab07 – A05 Injection: SQL injection (CWE-89) (POPRAVLJENO).
 *
 * POPRAVAK: svi upiti su parametrizirani ({@code ?} placeholderi). Za LIKE se
 * posebni znakovi ({@code %}, {@code _}, {@code \}) escapeaju, a uz upit se zadaje
 * ESCAPE znak. Duljina unosa je ogranicena.
 */
@RestController
@RequestMapping("/api/lab07")
public class Lab07Controller {

    private static final int MAX_QUERY_LENGTH = 100;

    private final JdbcTemplate jdbc;

    public Lab07Controller(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/products/search")
    public ResponseEntity<?> search(@RequestParam String q) {
        if (q.length() > MAX_QUERY_LENGTH) {
            return ResponseEntity.status(400).body(Map.of("error", "Preduga pretraga."));
        }
        // POPRAVAK: parametrizirani upit + escape LIKE metaznakova.
        String pattern = "%" + escapeLike(q) + "%";
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, name, description, price FROM lab07_products "
                        + "WHERE hidden = FALSE AND name LIKE ? ESCAPE '\\'",
                pattern);
        return ResponseEntity.ok(rows);
    }

    public record LoginRequest(String username, String password) {
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest body) {
        String u = body.username() == null ? "" : body.username();
        String p = body.password() == null ? "" : body.password();
        // POPRAVAK: parametrizirani upit; korisnicki unos se tretira kao podatak, ne kao SQL.
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, username FROM lab07_accounts WHERE username = ? AND password = ?", u, p);
        if (rows.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Neispravni podaci."));
        }
        return ResponseEntity.ok(Map.of("status", "prijavljen", "username", rows.get(0).get("username")));
    }

    /** Escapea LIKE metaznakove kako korisnik ne bi utjecao na uzorak pretrage. */
    private String escapeLike(String input) {
        return input.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
