package hr.span.akademija.vulnshop.lab06;

import hr.span.akademija.vulnshop.common.SeedData;
import java.util.List;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lab06 – A04 Cryptographic Failures (CWE-916, CWE-327, CWE-321, CWE-329).
 *
 * Dio A (lozinke): registracija/prijava spremaju MD5(hex, bez soli). Procurjeli
 * "dump" ({@code /customers/export}) omogucuje offline razbijanje jer su hashevi
 * brzi, bez soli i identicne lozinke daju identican hash.
 *
 * Dio B (podaci): brojevi kartica sifriraju se AES/ECB s HARDKODIRANIM kljucem u
 * kodu. ECB otkriva obrasce (isti ulaz -> isti ciphertext), a hardkodirani kljuc
 * znaci da tko god vidi kod moze desifrirati sve kartice.
 */
@RestController
@RequestMapping("/api/lab06")
public class Lab06Controller {

    // RANJIVO: hardkodirani 16-bajtni kljuc izravno u izvornom kodu.
    private static final byte[] HARDCODED_KEY = "AkademijaKljuc16".getBytes(StandardCharsets.UTF_8);

    private final JdbcTemplate jdbc;

    public Lab06Controller(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ----------------------------- Dio A: lozinke -----------------------------

    public record Credentials(String username, String password) {
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Credentials body) {
        if (body.username() == null || body.password() == null) {
            return ResponseEntity.status(400).body(Map.of("error", "Nedostaje korisnicko ime ili lozinka."));
        }
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM lab06_customers WHERE username = ?", Integer.class, body.username());
        if (count != null && count > 0) {
            return ResponseEntity.status(409).body(Map.of("error", "Korisnik vec postoji."));
        }
        // RANJIVO: MD5 bez soli.
        String hash = SeedData.md5Hex(body.password());
        jdbc.update("INSERT INTO lab06_customers (username, password_hash) VALUES (?, ?)",
                body.username(), hash);
        return ResponseEntity.ok(Map.of("username", body.username(), "status", "registriran"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Credentials body) {
        if (body.username() == null || body.password() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Neispravni podaci."));
        }
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT password_hash FROM lab06_customers WHERE username = ?", body.username());
        if (rows.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Neispravni podaci."));
        }
        String stored = (String) rows.get(0).get("password_hash");
        // RANJIVO: usporedba MD5 heksa.
        if (!stored.equals(SeedData.md5Hex(body.password()))) {
            return ResponseEntity.status(401).body(Map.of("error", "Neispravni podaci."));
        }
        return ResponseEntity.ok(Map.of("username", body.username(), "status", "prijavljen"));
    }

    /** RANJIVO: javni "export" cijele baze lozinki (simulira procurjeli dump), bez autentikacije. */
    @GetMapping("/customers/export")
    public ResponseEntity<?> export() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT username, password_hash FROM lab06_customers ORDER BY username");
        return ResponseEntity.ok(rows);
    }

    // ----------------------------- Dio B: kartice -----------------------------

    public record CardRequest(String cardNumber) {
    }

    @PostMapping("/cards")
    public ResponseEntity<?> storeCard(@RequestBody CardRequest body) {
        if (body.cardNumber() == null || body.cardNumber().length() < 4) {
            return ResponseEntity.status(400).body(Map.of("error", "Neispravan broj kartice."));
        }
        try {
            String ciphertext = encryptEcb(body.cardNumber());
            String last4 = body.cardNumber().substring(body.cardNumber().length() - 4);
            jdbc.update("INSERT INTO lab06_cards (owner, ciphertext, last4) VALUES (?, ?, ?)",
                    "demo", ciphertext, last4);
            Long id = jdbc.queryForObject("SELECT MAX(id) FROM lab06_cards", Long.class);
            return ResponseEntity.ok(Map.of("id", id, "ciphertext", ciphertext));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Sifriranje nije uspjelo."));
        }
    }

    @GetMapping("/cards")
    public ResponseEntity<?> listCards() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, ciphertext FROM lab06_cards ORDER BY id");
        return ResponseEntity.ok(rows);
    }

    @GetMapping("/cards/{id}/masked")
    public ResponseEntity<?> masked(@PathVariable long id) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT last4 FROM lab06_cards WHERE id = ?", id);
        if (rows.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Kartica nije pronadena."));
        }
        String last4 = (String) rows.get(0).get("last4");
        return ResponseEntity.ok(Map.of("masked", "**** **** **** " + last4));
    }

    /** RANJIVO: AES u ECB modu s hardkodiranim kljucem (deterministicki, bez IV-a). */
    private String encryptEcb(String plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(HARDCODED_KEY, "AES"));
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }
}
