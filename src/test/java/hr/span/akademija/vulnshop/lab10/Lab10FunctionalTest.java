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
 * Lab10 – funkcionalni testovi.
 */
@Tag("functional")
@Tag("lab10")
class Lab10FunctionalTest extends AbstractLabTest {

    private BigDecimal balance(String token) {
        ResponseEntity<Map> response = getWithToken("/api/lab10/wallet", token, Map.class);
        return new BigDecimal(String.valueOf(response.getBody().get("balance")));
    }

    @Test
    @DisplayName("Ispravan checkout (1x Kabel 9.99) umanjuje stanje za tocan iznos")
    void ispravanCheckoutUmanjujeStanje() {
        String token = login("ana", "Ana123!");
        BigDecimal prije = balance(token);
        ResponseEntity<Map> response = postWithToken("/api/lab10/checkout", token, Map.of(
                "items", List.of(Map.of("productId", 3, "quantity", 1, "unitPrice", 9.99)),
                "couponCodes", List.of()), Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        BigDecimal poslije = balance(token);
        // Mjeri razliku (stanje se dijeli medu testovima): kabel kosta 9.99.
        assertThat(prije.subtract(poslije)).isEqualByComparingTo("9.99");
    }
}
