package hr.span.akademija.vulnshop.lab15;

import hr.span.akademija.vulnshop.common.CurrentUser;
import java.math.BigDecimal;
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
 * Lab15 – A10 Mishandling of Exceptional Conditions (POPRAVLJENO).
 *
 * POPRAVAK:
 *  (1) FAIL-CLOSED: provjera limita vise se ne "guta"; svaka iznimka = odbijanje
 *      (vraca se 400, transfer se ne izvrsava).
 *  (2) TRANSAKCIJA: {@code @Transactional} osigurava da se terecenje poniusti ako
 *      odobrenje padne (nepostojeci odredisni racun) -> novac ne nestaje.
 *  (3) Validacija ulaza unaprijed (BigDecimal, valuta, iznos > 0) i genericke poruke
 *      greske bez detalja iznimke/SQL-a (vidi {@link Lab15ExceptionHandler}).
 */
@RestController
@RequestMapping("/api/lab15")
public class Lab15Controller {

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
    @Transactional
    public ResponseEntity<?> transfer(@RequestBody TransferRequest body, CurrentUser currentUser) {
        // POPRAVAK (3): validacija ulaza unaprijed.
        BigDecimal amount;
        try {
            amount = new BigDecimal(body.amount());
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", "Neispravan iznos."));
        }
        if (body.fromAccount() == null || body.toAccount() == null) {
            return ResponseEntity.status(400).body(Map.of("error", "Nedostaju podaci o racunima."));
        }

        // POPRAVAK (1): FAIL-CLOSED. Bilo koja iznimka u provjeri = odbijanje transfera.
        try {
            limitService.check(amount, body.currency());
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", "Transfer nije dopusten (provjera limita)."));
        }

        // Provjera vlasnistva i postojanja izvorisnog racuna.
        List<Map<String, Object>> from = jdbc.queryForList(
                "SELECT balance FROM lab15_accounts WHERE account_no = ? AND owner_username = ?",
                body.fromAccount(), currentUser.username());
        if (from.isEmpty()) {
            return ResponseEntity.status(400).body(Map.of("error", "Izvorisni racun nije dostupan."));
        }
        BigDecimal balance = new BigDecimal(String.valueOf(from.get(0).get("balance")));
        if (balance.compareTo(amount) < 0) {
            return ResponseEntity.status(400).body(Map.of("error", "Nedovoljno sredstava."));
        }

        // POPRAVAK (2): unutar transakcije. Terecenje pa odobrenje; ako odrediste ne postoji -> rollback.
        jdbc.update("UPDATE lab15_accounts SET balance = balance - ? WHERE account_no = ?",
                amount, body.fromAccount());
        int updated = jdbc.update("UPDATE lab15_accounts SET balance = balance + ? WHERE account_no = ?",
                amount, body.toAccount());
        if (updated == 0) {
            // Bacanje iznimke u @Transactional metodi ponistava i prethodno terecenje.
            throw new IllegalStateException("odredisni racun ne postoji");
        }
        return ResponseEntity.ok(Map.of("status", "izvrseno", "amount", amount));
    }
}
