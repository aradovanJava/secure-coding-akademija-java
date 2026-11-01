package hr.span.akademija.vulnshop.lab01;

import hr.span.akademija.vulnshop.common.CurrentUser;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lab01 – A01 Broken Access Control: IDOR (CWE-639).
 *
 * RANJIVO: endpoint dohvaca narudzbu po ID-u BEZ provjere vlasnistva, pa svaki
 * prijavljeni korisnik moze procitati tudu narudzbu (Insecure Direct Object Reference).
 */
@RestController
@RequestMapping("/api/lab01")
public class Lab01OrderController {

    private final JdbcTemplate jdbc;

    public Lab01OrderController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<?> getOrder(@PathVariable long id, CurrentUser currentUser) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, owner_username, items, shipping_address, total FROM lab01_orders WHERE id = ?",
                id);
        if (rows.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Narudzba nije pronadena."));
        }
        Map<String, Object> row = rows.get(0);
        // POPRAVAK: provjera vlasnistva. Ako narudzba nije korisnikova, a korisnik nije ADMIN,
        // vracamo 404 (ne 403) kako ne bismo otkrili postojanje tude narudzbe.
        String owner = (String) row.get("owner_username");
        if (!currentUser.username().equals(owner) && !currentUser.isAdmin()) {
            return ResponseEntity.status(404).body(Map.of("error", "Narudzba nije pronadena."));
        }
        return ResponseEntity.ok(toDto(row));
    }

    private Map<String, Object> toDto(Map<String, Object> row) {
        return Map.of(
                "id", row.get("id"),
                "owner", row.get("owner_username"),
                "items", row.get("items"),
                "shippingAddress", row.get("shipping_address"),
                "total", row.get("total"));
    }
}
