package hr.span.akademija.vulnshop.lab10;

import hr.span.akademija.vulnshop.common.CurrentUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lab10 – A06 Insecure Design: poslovna logika (CWE-840, CWE-20) (POPRAVLJENO).
 *
 * POPRAVAK:
 *  - cijena se UVIJEK uzima iz baze (klijentov {@code unitPrice} se ignorira),
 *  - kolicina mora biti 1..10,
 *  - najvise jedan kupon i jednokratna uporaba po korisniku,
 *  - ukupni iznos mora biti > 0 i stanje mora biti dostatno,
 *  - cijela operacija je u transakciji.
 */
@RestController
@RequestMapping("/api/lab10")
public class Lab10Controller {

    private static final BigDecimal COUPON_FACTOR = new BigDecimal("0.90");
    private static final String KNOWN_COUPON = "AKADEMIJA10";

    private final JdbcTemplate jdbc;

    public Lab10Controller(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/wallet")
    public ResponseEntity<?> wallet(CurrentUser currentUser) {
        BigDecimal balance = jdbc.queryForObject(
                "SELECT balance FROM lab10_wallets WHERE username = ?", BigDecimal.class, currentUser.username());
        return ResponseEntity.ok(Map.of("username", currentUser.username(), "balance", balance));
    }

    public record CheckoutItem(long productId, int quantity, BigDecimal unitPrice) {
    }

    public record CheckoutRequest(List<CheckoutItem> items, List<String> couponCodes) {
    }

    @PostMapping("/checkout")
    @Transactional
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequest body, CurrentUser currentUser) {
        if (body.items() == null || body.items().isEmpty()) {
            return ResponseEntity.status(400).body(Map.of("error", "Kosarica je prazna."));
        }

        BigDecimal total = BigDecimal.ZERO;
        for (CheckoutItem item : body.items()) {
            // POPRAVAK: kolicina mora biti 1..10.
            if (item.quantity() < 1 || item.quantity() > 10) {
                return ResponseEntity.status(400).body(Map.of("error", "Kolicina mora biti izmedu 1 i 10."));
            }
            // POPRAVAK: cijena iz baze, NE iz zahtjeva.
            BigDecimal price;
            try {
                price = jdbc.queryForObject(
                        "SELECT price FROM lab10_products WHERE id = ?", BigDecimal.class, item.productId());
            } catch (Exception e) {
                return ResponseEntity.status(400).body(Map.of("error", "Nepostojeci proizvod."));
            }
            total = total.add(price.multiply(BigDecimal.valueOf(item.quantity())));
        }

        // POPRAVAK: najvise jedan kupon, jednokratno po korisniku.
        if (body.couponCodes() != null && !body.couponCodes().isEmpty()) {
            if (body.couponCodes().size() > 1) {
                return ResponseEntity.status(400).body(Map.of("error", "Dopusten je najvise jedan kupon."));
            }
            String coupon = body.couponCodes().get(0);
            if (!KNOWN_COUPON.equalsIgnoreCase(coupon)) {
                return ResponseEntity.status(400).body(Map.of("error", "Nepoznat kupon."));
            }
            Integer used = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM lab10_coupon_usage WHERE username = ? AND coupon = ?",
                    Integer.class, currentUser.username(), KNOWN_COUPON);
            if (used != null && used > 0) {
                return ResponseEntity.status(400).body(Map.of("error", "Kupon je vec iskoristen."));
            }
            total = total.multiply(COUPON_FACTOR);
            jdbc.update("INSERT INTO lab10_coupon_usage (username, coupon) VALUES (?, ?)",
                    currentUser.username(), KNOWN_COUPON);
        }

        total = total.setScale(2, RoundingMode.HALF_UP);

        // POPRAVAK: ukupno mora biti > 0.
        if (total.signum() <= 0) {
            return ResponseEntity.status(400).body(Map.of("error", "Neispravan ukupni iznos."));
        }

        BigDecimal balance = jdbc.queryForObject(
                "SELECT balance FROM lab10_wallets WHERE username = ?", BigDecimal.class, currentUser.username());
        // POPRAVAK: provjera dostatnog stanja.
        if (balance.compareTo(total) < 0) {
            return ResponseEntity.status(400).body(Map.of("error", "Nedovoljno sredstava."));
        }

        BigDecimal newBalance = balance.subtract(total);
        jdbc.update("UPDATE lab10_wallets SET balance = ? WHERE username = ?", newBalance, currentUser.username());

        return ResponseEntity.ok(Map.of("charged", total, "balance", newBalance));
    }
}
