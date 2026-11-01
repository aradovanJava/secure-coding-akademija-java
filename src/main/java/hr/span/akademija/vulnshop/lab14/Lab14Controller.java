package hr.span.akademija.vulnshop.lab14;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
 * Lab14 – A09 Security Logging & Alerting Failures (POPRAVLJENO).
 *
 * POPRAVAK:
 *  - lozinka se NIKAD ne logira,
 *  - korisnicko ime se neutralizira (CR/LF -> '_') prije logiranja (sprjecava log injection),
 *  - neuspjela prijava biljezi se kao SECURITY_EVENT ... LOGIN_FAILED (WARN), uspjeh kao LOGIN_SUCCESS (INFO),
 *  - nakon 5 neuspjeha za istog korisnika u 5 min biljezi se SECURITY_ALERT BRUTE_FORCE_SUSPECTED (ERROR).
 */
@RestController
@RequestMapping("/api/lab14")
public class Lab14Controller {

    private static final Logger log = LoggerFactory.getLogger(Lab14Controller.class);
    private static final int ALERT_THRESHOLD = 5;
    private static final Duration WINDOW = Duration.ofMinutes(5);

    private final JdbcTemplate jdbc;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final ConcurrentHashMap<String, FailWindow> failures = new ConcurrentHashMap<>();

    public Lab14Controller(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final class FailWindow {
        int count;
        Instant windowStart = Instant.now();
    }

    public record LoginRequest(String username, String password) {
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest body, HttpServletRequest request) {
        String rawUsername = body.username() == null ? "" : body.username();
        String password = body.password() == null ? "" : body.password();
        // POPRAVAK: neutraliziraj CR/LF (i ostale kontrolne znakove) prije logiranja.
        String safeUser = sanitize(rawUsername);
        String ip = sanitize(request.getRemoteAddr());

        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT password FROM users WHERE username = ?", rawUsername);
        boolean ok = !rows.isEmpty() && encoder.matches(password, (String) rows.get(0).get("password"));

        if (!ok) {
            // POPRAVAK: sigurnosni dogadaj za neuspjeh (bez lozinke).
            log.warn("SECURITY_EVENT LOGIN_FAILED user={} ip={}", safeUser, ip);
            registerFailureAndMaybeAlert(safeUser, ip);
            return ResponseEntity.status(401).body(Map.of("error", "Neispravni podaci."));
        }
        failures.remove(safeUser);
        log.info("SECURITY_EVENT LOGIN_SUCCESS user={} ip={}", safeUser, ip);
        return ResponseEntity.ok(Map.of("status", "prijavljen", "username", rawUsername));
    }

    private void registerFailureAndMaybeAlert(String safeUser, String ip) {
        FailWindow fw = failures.computeIfAbsent(safeUser, k -> new FailWindow());
        synchronized (fw) {
            if (Instant.now().isAfter(fw.windowStart.plus(WINDOW))) {
                fw.count = 0;
                fw.windowStart = Instant.now();
            }
            fw.count++;
            if (fw.count >= ALERT_THRESHOLD) {
                log.error("SECURITY_ALERT BRUTE_FORCE_SUSPECTED user={} ip={} pokusaja={}",
                        safeUser, ip, fw.count);
            }
        }
    }

    /** Zamjenjuje CR/LF i ostale kontrolne znakove s '_' (zastita od log injectiona, CWE-117). */
    private String sanitize(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            sb.append(Character.isISOControl(c) ? '_' : c);
        }
        return sb.toString();
    }
}
