package hr.span.akademija.vulnshop.lab15;

import java.math.BigDecimal;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Lab15 – servis koji provjerava dnevni limit transakcije.
 *
 * Baca iznimku za nepoznatu valutu ili neispravan iznos. Poanta vjezbe je da
 * RANJIVI kontroler ovu iznimku "proguta" (fail-open) umjesto da odbije transakciju.
 */
@Service
public class LimitService {

    private static final Set<String> ALLOWED_CURRENCIES = Set.of("EUR", "USD");
    private static final BigDecimal DAILY_LIMIT = new BigDecimal("1000.00");

    /**
     * Provjerava je li transfer unutar dopustenih pravila.
     *
     * @throws IllegalArgumentException za nepoznatu valutu ili neispravan iznos
     * @throws LimitExceededException   ako iznos premasuje dnevni limit
     */
    public void check(BigDecimal amount, String currency) {
        if (currency == null || !ALLOWED_CURRENCIES.contains(currency)) {
            throw new IllegalArgumentException("Nepoznata valuta: " + currency);
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Neispravan iznos: " + amount);
        }
        if (amount.compareTo(DAILY_LIMIT) > 0) {
            throw new LimitExceededException("Prekoracen dnevni limit od " + DAILY_LIMIT + " EUR.");
        }
    }

    public static class LimitExceededException extends RuntimeException {
        public LimitExceededException(String message) {
            super(message);
        }
    }
}
