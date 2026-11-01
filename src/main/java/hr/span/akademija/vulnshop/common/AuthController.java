package hr.span.akademija.vulnshop.common;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Zajednicka autentikacija (nije dio vjezbi, implementirana ISPRAVNO).
 *
 * {@code POST /api/auth/login {"username","password"}} -> 200 s nasumicnim tokenom
 * ako su podaci tocni, inace 401. Lozinke u tablici {@code users} pohranjene su
 * kao BCrypt hash (vidi {@link SeedData}).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JdbcTemplate jdbc;
    private final TokenStore tokenStore;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthController(JdbcTemplate jdbc, TokenStore tokenStore) {
        this.jdbc = jdbc;
        this.tokenStore = tokenStore;
    }

    public record LoginRequest(String username, String password) {
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request == null || request.username() == null || request.password() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Neispravno korisnicko ime ili lozinka."));
        }
        var rows = jdbc.queryForList(
                "SELECT id, username, password, role FROM users WHERE username = ?",
                request.username());
        if (rows.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Neispravno korisnicko ime ili lozinka."));
        }
        var row = rows.get(0);
        String storedHash = (String) row.get("password");
        if (!encoder.matches(request.password(), storedHash)) {
            return ResponseEntity.status(401).body(Map.of("error", "Neispravno korisnicko ime ili lozinka."));
        }
        CurrentUser user = new CurrentUser(
                ((Number) row.get("id")).longValue(),
                (String) row.get("username"),
                (String) row.get("role"));
        String token = tokenStore.issue(user);
        return ResponseEntity.ok(Map.of(
                "token", token,
                "username", user.username(),
                "role", user.role()));
    }
}
