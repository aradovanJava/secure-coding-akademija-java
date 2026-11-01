package hr.span.akademija.vulnshop.lab02;

import hr.span.akademija.vulnshop.common.CurrentUser;
import hr.span.akademija.vulnshop.common.ForbiddenException;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lab02 – A01 Broken Access Control: Mass assignment / eskalacija privilegija
 * (CWE-915, CWE-269).
 *
 * RANJIVO: {@code PUT /profile} prima cijeli objekt profila, ukljucujuci polje
 * {@code role}, i zapisuje ga u bazu. Korisnik tako moze sam sebi postaviti
 * ulogu ADMIN (mass assignment) i dobiti pristup administratorskom endpointu.
 */
@RestController
@RequestMapping("/api/lab02")
public class Lab02ProfileController {

    private final JdbcTemplate jdbc;

    public Lab02ProfileController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** RANJIVO: binda se i {@code role} iz tijela zahtjeva. */
    public record ProfileUpdate(String displayName, String email, String role) {
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(CurrentUser currentUser) {
        Map<String, Object> row = loadProfile(currentUser.id());
        return ResponseEntity.ok(toDto(row));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody ProfileUpdate update, CurrentUser currentUser) {
        // RANJIVO: role dolazi izravno iz JSON-a i zapisuje se bez provjere (mass assignment).
        // Ako klijent ne posalje role, zadrzava se postojeca uloga (COALESCE) - legitimni klijent
        // ne salje role, ali napadac moze poslati role=ADMIN i eskalirati privilegije.
        jdbc.update(
                "UPDATE lab02_profiles SET display_name = ?, email = ?, role = COALESCE(?, role) WHERE user_id = ?",
                update.displayName(), update.email(), update.role(), currentUser.id());
        return ResponseEntity.ok(toDto(loadProfile(currentUser.id())));
    }

    @GetMapping("/admin/users")
    public ResponseEntity<?> adminUsers(CurrentUser currentUser) {
        // Ovlast se provjerava prema ulozi iz tablice profila (koju je napadac mogao promijeniti).
        String role = jdbc.queryForObject(
                "SELECT role FROM lab02_profiles WHERE user_id = ?", String.class, currentUser.id());
        if (!"ADMIN".equals(role)) {
            throw new ForbiddenException("Samo administrator smije vidjeti popis korisnika.");
        }
        List<Map<String, Object>> users = jdbc.queryForList(
                "SELECT user_id, display_name, email, role FROM lab02_profiles ORDER BY user_id");
        return ResponseEntity.ok(users);
    }

    private Map<String, Object> loadProfile(long userId) {
        return jdbc.queryForMap(
                "SELECT user_id, display_name, email, role FROM lab02_profiles WHERE user_id = ?", userId);
    }

    private Map<String, Object> toDto(Map<String, Object> row) {
        return Map.of(
                "userId", row.get("user_id"),
                "displayName", row.get("display_name"),
                "email", row.get("email"),
                "role", row.get("role"));
    }
}
