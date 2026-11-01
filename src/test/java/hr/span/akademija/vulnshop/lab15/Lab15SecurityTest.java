package hr.span.akademija.vulnshop.lab15;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.vulnshop.common.AbstractLabTest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Lab15 – sigurnosni testovi (rukovanje iznimkama: fail-open, nema transakcije,
 * procurivanje detalja greske).
 *
 * Saldo se mjeri kao RAZLIKA (prije/poslije) jer se baza dijeli medu testovima.
 */
@Tag("security")
@Tag("lab15")
class Lab15SecurityTest extends AbstractLabTest {

    @SuppressWarnings("unchecked")
    private BigDecimal balanceOf(String token, String accountNo) {
        ResponseEntity<List> response = getWithToken("/api/lab15/accounts", token, List.class);
        List<Map<String, Object>> rows = response.getBody();
        for (Map<String, Object> row : rows) {
            Object no = row.get("account_no");
            if (no == null) {
                no = row.get("ACCOUNT_NO");
            }
            if (accountNo.equals(no)) {
                Object bal = row.get("balance");
                if (bal == null) {
                    bal = row.get("BALANCE");
                }
                return new BigDecimal(String.valueOf(bal));
            }
        }
        throw new IllegalStateException("Racun nije pronaden: " + accountNo);
    }

    @Test
    @DisplayName("Transfer 50000 s nepoznatom valutom XYZ mora biti odbijen (fail-closed), saldo nepromijenjen")
    void failClosedNepoznataValuta() {
        String token = login("ana", "Ana123!");
        BigDecimal prije = balanceOf(token, "HR-ANA-001");
        ResponseEntity<String> response = postWithToken("/api/lab15/transfer", token, Map.of(
                "fromAccount", "HR-ANA-001", "toAccount", "HR-MARKO-001",
                "amount", "50000.00", "currency", "XYZ"), String.class);
        BigDecimal poslije = balanceOf(token, "HR-ANA-001");

        assertThat(response.getStatusCode().value())
                .as("Transfer s nepoznatom valutom nije odbijen (fail-open)")
                .isBetween(400, 499);
        assertThat(poslije)
                .as("Saldo je promijenjen unatoc neuspjeloj provjeri limita")
                .isEqualByComparingTo(prije);
    }

    @Test
    @DisplayName("Transfer na nepostojeci racun mora ostaviti saldo nepromijenjen (transakcija)")
    void nemaGubitkaBezTransakcije() {
        String token = login("ana", "Ana123!");
        BigDecimal prije = balanceOf(token, "HR-ANA-001");
        ResponseEntity<String> response = postWithToken("/api/lab15/transfer", token, Map.of(
                "fromAccount", "HR-ANA-001", "toAccount", "HR-NEPOSTOJI-999",
                "amount", "100.00", "currency", "EUR"), String.class);
        BigDecimal poslije = balanceOf(token, "HR-ANA-001");

        assertThat(poslije)
                .as("Novac je nestao: terecenje nije ponisteno kad je odobrenje palo (nema transakcije)")
                .isEqualByComparingTo(prije);
        assertThat(response.getStatusCode().value()).isBetween(400, 499);
    }

    @Test
    @DisplayName("Tijelo greske ne smije sadrzavati naziv klase iznimke ni SQL")
    void greskaNeProcurujeDetalje() {
        String token = login("ana", "Ana123!");
        ResponseEntity<String> response = postWithToken("/api/lab15/transfer", token, Map.of(
                "fromAccount", "HR-ANA-001", "toAccount", "HR-NEPOSTOJI-999",
                "amount", "100.00", "currency", "EUR"), String.class);
        String body = response.getBody() == null ? "" : response.getBody();
        assertThat(body)
                .as("Detalji iznimke/SQL procurili u odgovoru (CWE-209)")
                .doesNotContain("Exception")
                .doesNotContain("lab15_accounts")
                .doesNotContain("java.lang")
                .doesNotContain("java.sql");
    }
}
