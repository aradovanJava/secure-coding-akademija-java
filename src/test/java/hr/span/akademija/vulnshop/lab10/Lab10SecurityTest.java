package hr.span.akademija.vulnshop.lab10;

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
 * Lab10 – sigurnosni testovi (poslovna logika).
 *
 * Testovi mjere RAZLIKU stanja novcanika (prije/poslije), jer se kontekst i baza
 * dijele medu testovima.
 */
@Tag("security")
@Tag("lab10")
class Lab10SecurityTest extends AbstractLabTest {

    private BigDecimal balance(String token) {
        ResponseEntity<Map> response = getWithToken("/api/lab10/wallet", token, Map.class);
        return new BigDecimal(String.valueOf(response.getBody().get("balance")));
    }

    @Test
    @DisplayName("Negativna količina (-5) mora vratiti 400 i NE smije povećati stanje")
    void negativnaKolicinaOdbijena() {
        String token = login("marko", "Marko123!");
        BigDecimal prije = balance(token);
        ResponseEntity<Map> response = postWithToken("/api/lab10/checkout", token, Map.of(
                "items", List.of(Map.of("productId", 1, "quantity", -5, "unitPrice", 899.00)),
                "couponCodes", List.of()), Map.class);
        BigDecimal poslije = balance(token);

        boolean odbijeno = response.getStatusCode().value() == 400;
        boolean stanjeNijeNaraslo = poslije.compareTo(prije) <= 0;
        assertThat(odbijeno && stanjeNijeNaraslo)
                .as("Negativna kolicina prosla (status=%s, prije=%s, poslije=%s)",
                        response.getStatusCode().value(), prije, poslije)
                .isTrue();
    }

    @Test
    @DisplayName("Lažnom cijenom 0.01 za laptop ne smije se naplatiti 0.01 (cijena dolazi iz baze)")
    void laznaCijenaIgnorirana() {
        String token = login("ana", "Ana123!");
        ResponseEntity<Map> response = postWithToken("/api/lab10/checkout", token, Map.of(
                "items", List.of(Map.of("productId", 1, "quantity", 1, "unitPrice", 0.01)),
                "couponCodes", List.of()), Map.class);
        int status = response.getStatusCode().value();
        if (status == 200) {
            BigDecimal charged = new BigDecimal(String.valueOf(response.getBody().get("charged")));
            assertThat(charged)
                    .as("Naplaceno po laznoj cijeni 0.01 umjesto cijene iz baze (899.00)")
                    .isEqualByComparingTo("899.00");
        } else {
            assertThat(status).as("Ocekivan 200 (naplata iz baze) ili 400 (odbijeno)").isEqualTo(400);
        }
    }

    @Test
    @DisplayName("Trostruki isti kupon mora biti odbijen (400) ili popust primijenjen samo jednom")
    void visestrukiKuponOdbijen() {
        String token = login("admin", "Admin123!");
        ResponseEntity<Map> response = postWithToken("/api/lab10/checkout", token, Map.of(
                "items", List.of(Map.of("productId", 2, "quantity", 1, "unitPrice", 99.00)),
                "couponCodes", List.of("AKADEMIJA10", "AKADEMIJA10", "AKADEMIJA10")), Map.class);
        int status = response.getStatusCode().value();
        if (status == 200) {
            BigDecimal charged = new BigDecimal(String.valueOf(response.getBody().get("charged")));
            // Proizvod 2 (Slusalice) kosta 99.00; jedan popust 10% -> 89.10. Trobostruki popust -> ~72.17.
            assertThat(charged)
                    .as("Kupon je primijenjen vise puta (naplaceno %s)", charged)
                    .isGreaterThanOrEqualTo(new BigDecimal("89.10"));
        } else {
            assertThat(status).isEqualTo(400);
        }
    }
}
