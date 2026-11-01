package hr.span.akademija.vulnshop.lab15;

import hr.span.akademija.vulnshop.common.CurrentUser;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lab15 – A10 Mishandling of Exceptional Conditions (CWE-636, CWE-755, CWE-209, CWE-460).
 *
 * RANJIVO:
 *  (1) FAIL-OPEN: iznimka iz {@link LimitService#check} se hvata i ignorira, pa
 *      transfer s nepoznatom valutom (npr. XYZ) i iznosom iznad limita prolazi.
 *  (2) NEMA TRANSAKCIJE: terecenje se izvrsi, a ako odobrenje (uplata na
 *      nepostojeci racun) padne, novac nestaje.
 *  (3) Poruka greske vraca detalje iznimke (procurivanje informacija).
 */
@RestController
@RequestMapping("/api/lab15")
public class Lab15Controller {

    private static final Logger log = LoggerFactory.getLogger(Lab15Controller.class);

    private final JdbcTemplate jdbc;
    private final LimitService limitService;

    public Lab15Controller(JdbcTemplate jdbc, LimitService limitService) {
        this.jdbc = jdbc;
        this.limitService = limitService;
    }

    @GetMapping("/accounts")
    public ResponseEntity<?> accounts(CurrentUser currentUser) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT account_no, balance FROM lab15_accounts WHERE owner_username = ? ORDER BY account_no",
                currentUser.username());
        return ResponseEntity.ok(rows);
    }

    public record TransferRequest(String fromAccount, String toAccount, String amount, String currency) {
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@RequestBody TransferRequest body, CurrentUser currentUser) {
        try {
            BigDecimal amount = new BigDecimal(body.amount());

            // RANJIVO (1) FAIL-OPEN: iznimka u sigurnosnoj provjeri se guta.
            try {
                limitService.check(amount, body.currency());
            } catch (Exception e) {
                log.debug("Provjera limita nije uspjela, nastavljam svejedno: {}", e.getMessage());
            }

            // RANJIVO (2): terecenje i odobrenje bez transakcije.
            jdbc.update("UPDATE lab15_accounts SET balance = balance - ? WHERE account_no = ?",
                    amount, body.fromAccount());
            // Odobrenje na odredisni racun; ako ne postoji, azurira 0 redaka -> novac "nestane".
            int updated = jdbc.update("UPDATE lab15_accounts SET balance = balance + ? WHERE account_no = ?",
                    amount, body.toAccount());
            if (updated == 0) {
                // RANJIVO (3): detaljna poruka + terecenje se NE ponistava.
                throw new IllegalStateException(
                        "Odredisni racun " + body.toAccount() + " ne postoji u tablici lab15_accounts");
            }
            return ResponseEntity.ok(Map.of("status", "izvrseno", "amount", amount));
        } catch (Exception e) {
            // RANJIVO (3): vraca se naziv klase iznimke i detaljna poruka.
            return ResponseEntity.status(500).body(Map.of(
                    "error", e.getClass().getName() + ": " + e.getMessage()));
        }
    }
}
