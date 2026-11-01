package hr.span.akademija.vulnshop.lab10;

import hr.span.akademija.vulnshop.common.CurrentUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lab10 – A06 Insecure Design: poslovna logika (CWE-840, CWE-20).
 *
 * RANJIVO: server vjeruje cijeni ({@code unitPrice}) i kolicini ({@code quantity})
 * iz zahtjeva (dopusta i negativne), primjenjuje SVAKI kupon iz liste (pa i isti
 * vise puta), i mijenja stanje novcanika bez ikakve provjere. Negativna kolicina
 * tako POVECAVA stanje, a pretplacivanjem kupona iznos se umjetno obara.
 */
@RestController
@RequestMapping("/api/lab10")
public class Lab10Controller {

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
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequest body, CurrentUser currentUser) {
        BigDecimal total = BigDecimal.ZERO;
        // RANJIVO: koristi se unitPrice i quantity iz zahtjeva (bez provjere predznaka).
        for (CheckoutItem item : body.items()) {
            total = total.add(item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())));
        }
        // RANJIVO: svaki kupon iz liste primjenjuje se zasebno (moze i vise puta).
        if (body.couponCodes() != null) {
            for (String coupon : body.couponCodes()) {
                if ("AKADEMIJA10".equalsIgnoreCase(coupon)) {
                    total = total.multiply(new BigDecimal("0.90"));
                }
            }
        }
        total = total.setScale(2, RoundingMode.HALF_UP);

        BigDecimal balance = jdbc.queryForObject(
                "SELECT balance FROM lab10_wallets WHERE username = ?", BigDecimal.class, currentUser.username());
        // RANJIVO: nema provjere total > 0 ni dostatnog stanja; negativan total povecava stanje.
        BigDecimal newBalance = balance.subtract(total);
        jdbc.update("UPDATE lab10_wallets SET balance = ? WHERE username = ?", newBalance, currentUser.username());

        return ResponseEntity.ok(Map.of(
                "charged", total,
                "balance", newBalance));
    }
}
