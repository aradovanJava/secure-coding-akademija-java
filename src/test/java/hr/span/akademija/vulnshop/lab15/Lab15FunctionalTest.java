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
 * Lab15 – funkcionalni testovi.
 */
@Tag("functional")
@Tag("lab15")
class Lab15FunctionalTest extends AbstractLabTest {

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
    @DisplayName("Ispravan transfer 100 EUR vraća 200 i smanjuje saldo za 100 (mjeri razliku)")
    void ispravanTransfer() {
        String token = login("ana", "Ana123!");
        BigDecimal prije = balanceOf(token, "HR-ANA-001");
        ResponseEntity<Map> response = postWithToken("/api/lab15/transfer", token, Map.of(
                "fromAccount", "HR-ANA-001", "toAccount", "HR-MARKO-001",
                "amount", "100.00", "currency", "EUR"), Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        BigDecimal poslije = balanceOf(token, "HR-ANA-001");
        assertThat(prije.subtract(poslije)).isEqualByComparingTo("100.00");
    }
}
