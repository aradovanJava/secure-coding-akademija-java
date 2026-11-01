package hr.span.akademija.vulnshop.lab14;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lab14 – A09 Security Logging & Alerting Failures (CWE-778, CWE-117, CWE-532).
 *
 * RANJIVO:
 *  - lozinka se zapisuje u log (CWE-532),
 *  - korisnicko ime se zapisuje bez neutralizacije CR/LF, pa napadac moze
 *    krivotvoriti lazne retke u logu (log injection, CWE-117),
 *  - neuspjela prijava se NE biljezi kao sigurnosni dogadaj (CWE-778),
 *    pa nema ni detekcije brute-force napada.
 */
@RestController
@RequestMapping("/api/lab14")
public class Lab14Controller {

    private static final Logger log = LoggerFactory.getLogger(Lab14Controller.class);

    private final JdbcTemplate jdbc;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public Lab14Controller(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record LoginRequest(String username, String password) {
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest body) {
        String username = body.username() == null ? "" : body.username();
        String password = body.password() == null ? "" : body.password();

        // RANJIVO: lozinka u logu + korisnicko ime bez neutralizacije CR/LF.
        log.info("Login attempt user=" + username + " password=" + password);

        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT password FROM users WHERE username = ?", username);
        if (rows.isEmpty() || !encoder.matches(password, (String) rows.get(0).get("password"))) {
            // RANJIVO: neuspjeh se ne biljezi kao sigurnosni dogadaj (nema ni detekcije brute-forcea).
            return ResponseEntity.status(401).body(Map.of("error", "Neispravni podaci."));
        }
        log.info("Login successful user=" + username);
        return ResponseEntity.ok(Map.of("status", "prijavljen", "username", username));
    }
}
