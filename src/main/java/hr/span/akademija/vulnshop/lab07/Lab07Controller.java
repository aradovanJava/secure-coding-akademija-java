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
 * Lab07 – A05 Injection: SQL injection (CWE-89).
 *
 * RANJIVO: i pretraga proizvoda i prijava grade SQL upit konkatenacijom
 * korisnickog unosa. Napadac moze dohvatiti skrivene proizvode
 * ({@code ' OR '1'='1' --}), izvuci podatke drugih tablica (UNION) ili zaobici
 * prijavu ({@code admin'--}).
 */
@RestController
@RequestMapping("/api/lab07")
public class Lab07Controller {

    private final JdbcTemplate jdbc;

    public Lab07Controller(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/products/search")
    public ResponseEntity<?> search(@RequestParam String q) {
        // RANJIVO: q se umece izravno u SQL.
        String sql = "SELECT id, name, description, price FROM lab07_products "
                + "WHERE hidden = FALSE AND name LIKE '%" + q + "%'";
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(sql);
            return ResponseEntity.ok(rows);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", "Greska u upitu."));
        }
    }

    public record LoginRequest(String username, String password) {
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest body) {
        String u = body.username() == null ? "" : body.username();
        String p = body.password() == null ? "" : body.password();
        // RANJIVO: username i password se umecu izravno u SQL.
        String sql = "SELECT id, username FROM lab07_accounts WHERE username = '" + u
                + "' AND password = '" + p + "'";
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(sql);
            if (rows.isEmpty()) {
                return ResponseEntity.status(401).body(Map.of("error", "Neispravni podaci."));
            }
            return ResponseEntity.ok(Map.of("status", "prijavljen", "username", rows.get(0).get("username")));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Neispravni podaci."));
        }
    }
}
