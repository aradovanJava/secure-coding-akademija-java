package hr.span.akademija.vulnshop.lab06;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lab06 – A04 Cryptographic Failures (POPRAVLJENO).
 *
 * Dio A (lozinke): BCrypt (cost 12) iz spring-security-crypto. Soljen i spor hash,
 * pa iste lozinke daju razlicite hasheve i offline razbijanje je neisplativo.
 *
 * Dio B (podaci): AES/GCM/NoPadding s nasumicnim 12-bajtnim IV-om po zapisu; pohranjuje
 * se {@code IV || ciphertext+tag}. Kljuc dolazi iz konfiguracije/okoline
 * ({@code app.lab06.encryption-key}), a ne iz izvornog koda.
 */
@RestController
@RequestMapping("/api/lab06")
public class Lab06Controller {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;

    private final JdbcTemplate jdbc;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
    private final byte[] key;
    private final SecureRandom random = new SecureRandom();

    public Lab06Controller(JdbcTemplate jdbc,
                           @Value("${app.lab06.encryption-key}") String base64Key) {
        this.jdbc = jdbc;
        // POPRAVAK: kljuc iz konfiguracije/okoline (Base64), nije hardkodiran u kodu.
        this.key = Base64.getDecoder().decode(base64Key);
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
        // POPRAVAK: BCrypt umjesto MD5.
        String hash = encoder.encode(body.password());
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
        // POPRAVAK: BCrypt provjera.
        if (!encoder.matches(body.password(), stored)) {
            return ResponseEntity.status(401).body(Map.of("error", "Neispravni podaci."));
        }
        return ResponseEntity.ok(Map.of("username", body.username(), "status", "prijavljen"));
    }

    /**
     * Napomena: "export" i dalje postoji za demonstraciju, ali sada su hashevi BCrypt
     * (soljeni, spori) pa procurjeli dump nije lako razbiti. U stvarnoj aplikaciji
     * ovakav javni endpoint ionako ne bi smio postojati.
     */
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
            String ciphertext = encryptGcm(body.cardNumber());
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

    /** POPRAVAK: AES-GCM s nasumicnim IV-om; vraca Base64(IV || ciphertext+tag). */
    private String encryptGcm(String plaintext) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        random.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
        return Base64.getEncoder().encodeToString(combined);
    }
}
