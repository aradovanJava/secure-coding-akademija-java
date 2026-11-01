package hr.span.akademija.vulnshop.lab12;

import hr.span.akademija.vulnshop.common.CurrentUser;
import hr.span.akademija.vulnshop.common.TokenStore;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lab12 – A07 Authentication Failures: JWT propusti (CWE-347, CWE-798, CWE-613).
 *
 * Koristi {@link JwtService} s RANJIVIM verifikatorom (alg:none, bez provjere exp,
 * usporedba potpisa obicnim equals) i slabom hardkodiranom tajnom.
 *
 * Napomena: ovaj lab NE koristi zajednicki {@link TokenStore}/Bearer mehanizam,
 * nego vlastiti JWT u istom {@code Authorization: Bearer} zaglavlju.
 */
@RestController
@RequestMapping("/api/lab12")
public class Lab12Controller {

    private final JdbcTemplate jdbc;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public Lab12Controller(JdbcTemplate jdbc, JwtService jwtService) {
        this.jdbc = jdbc;
        this.jwtService = jwtService;
    }

    public record LoginRequest(String username, String password) {
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest body) {
        if (body.username() == null || body.password() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Neispravni podaci."));
        }
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT username, password, role FROM users WHERE username = ?", body.username());
        if (rows.isEmpty() || !encoder.matches(body.password(), (String) rows.get(0).get("password"))) {
            return ResponseEntity.status(401).body(Map.of("error", "Neispravni podaci."));
        }
        String role = (String) rows.get(0).get("role");
        String token = jwtService.createToken(body.username(), role, Instant.now().plus(15, ChronoUnit.MINUTES));
        return ResponseEntity.ok(Map.of("token", token, "role", role));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(value = "Authorization", required = false) String auth) {
        JwtService.VerifiedToken verified = verify(auth);
        if (verified == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Neispravan token."));
        }
        return ResponseEntity.ok(Map.of("sub", verified.subject(), "role", verified.role()));
    }

    @GetMapping("/admin")
    public ResponseEntity<?> admin(@RequestHeader(value = "Authorization", required = false) String auth) {
        JwtService.VerifiedToken verified = verify(auth);
        if (verified == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Neispravan token."));
        }
        if (!"ADMIN".equals(verified.role())) {
            return ResponseEntity.status(403).body(Map.of("error", "Samo administrator."));
        }
        return ResponseEntity.ok(Map.of("status", "admin ok", "sub", verified.subject()));
    }

    private JwtService.VerifiedToken verify(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring("Bearer ".length()).trim();
        // RANJIVO: koristi se verifyVulnerable.
        return jwtService.verifyVulnerable(token);
    }
}
